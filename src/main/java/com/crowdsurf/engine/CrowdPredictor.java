package com.crowdsurf.engine;

import com.crowdsurf.config.HeuristicConfig;
import com.crowdsurf.domain.CrowdPrediction;
import com.crowdsurf.domain.Forecast;
import com.crowdsurf.domain.Spot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor heurístico unificado para predicción de crowd en spots de surf.
 * 
 * Consolida los 5 factores del modelo original:
 * 1. Factor Temporal (día, hora, estación)
 * 2. Factor Oleaje (altura, periodo, dirección)
 * 3. Factor Viento (velocidad, dirección, offshore/onshore)
 * 4. Factor Marea (estado actual vs preferencia del spot)
 * 5. Factor Clima (lluvia, temperatura)
 * 
 * Fórmula general:
 * Score = BasePopularity × StokeMultiplier × FactoresTemporales × FactoresClima
 * 
 * El Score se normaliza a una escala 0-10 y se categoriza:
 * 0-2: EMPTY, 2-4: LOW, 4-7: MEDIUM, 7-9: HIGH, 9-10: SATURATED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrowdPredictor {

    private final HeuristicConfig config;

    /**
     * Genera predicción de crowd para un spot dado un pronóstico meteorológico.
     * 
     * @param spot     Spot de surf con su configuración
     * @param forecast Condiciones meteorológicas en el momento
     * @return Predicción con nivel 0-10, categoría y factores
     */
    public CrowdPrediction predict(Spot spot, Forecast forecast) {

        // 1. MODO NOCTURNO: De noche no hay nadie surfeando
        if (!isDaylight(forecast)) {
            return buildNightPrediction(spot, forecast);
        }

        // 2. BASE: Popularidad del spot como punto de partida
        double basePopularity = spot.getBasePopularity() != null ? spot.getBasePopularity() : 5.0;
        double baseScore = 2.0 + (basePopularity * 0.6);

        // 3. FACTORES: Acumulamos multiplicadores y explicaciones
        List<String> factors = new ArrayList<>();
        double multiplier = 1.0;

        // 3.1 Factor Temporal
        double timeFactor = calculateTimeFactor(forecast, factors);
        multiplier *= timeFactor;

        // 3.2 Factor Calidad de Olas
        double waveQuality = calculateWaveQuality(spot, forecast, factors);

        // 3.3 Factor Viento
        double windQuality = calculateWindQuality(spot, forecast, factors);

        // 3.4 Factor Marea
        double tideFactor = calculateTideFactor(spot, forecast, factors);
        multiplier *= tideFactor;

        // 3.5 Factor Clima
        double weatherFactor = calculateWeatherFactor(forecast, factors);
        multiplier *= weatherFactor;

        // 4. STOKE: Calidad efectiva → multiplicador sigmoid
        double effectiveQuality = (waveQuality * config.getWaveQualityWeight())
                + (windQuality * config.getWindScoreWeight());
        double stokeMultiplier = calculateSigmoidStoke(effectiveQuality * 10.0);

        // 5. SCORE FINAL
        double rawScore = baseScore * stokeMultiplier * multiplier;

        // Validación
        if (Double.isNaN(rawScore) || rawScore < 0) {
            rawScore = 0.0;
        }

        // 6. NORMALIZACIÓN (0-10)
        double normalizedScore = Math.min(10.0, (rawScore / config.getMaxRawScore()) * 10.0);
        int crowdLevel = (int) Math.round(normalizedScore);

        // 7. CONFIANZA: Disminuye con el tiempo
        double confidence = calculateConfidence(forecast);

        // 8. CONSTRUIR PREDICCIÓN
        return CrowdPrediction.builder()
                .spot(spot)
                .predictionTime(forecast.getTimestamp())
                .crowdLevel(crowdLevel)
                .crowdCategory(determineCategory(normalizedScore))
                .confidence(confidence)
                .conditionQuality(effectiveQuality)
                .rawScore(rawScore)
                .tideHeight(forecast.getTideHeight())
                .tideState(forecast.getTideState())
                .primaryFactors(String.join(", ", factors))
                .build();
    }

    // ========================================================================
    // CÁLCULO DE FACTORES
    // ========================================================================

    /**
     * Calcula el factor temporal basado en día de la semana y hora.
     */
    private double calculateTimeFactor(Forecast f, List<String> factors) {
        LocalDateTime time = f.getTimestamp();
        DayOfWeek day = time.getDayOfWeek();
        int hour = time.getHour();
        int month = time.getMonthValue();

        double multiplier = 1.0;

        // Día de la semana
        boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
        boolean isHoliday = Boolean.TRUE.equals(f.getIsHoliday());

        if (isWeekend || isHoliday) {
            multiplier *= config.getWeekendMultiplier();
            factors.add("Fin de semana");
        } else {
            // Días laborables: penalización en horario de trabajo
            if (hour >= 9 && hour <= 17) {
                multiplier *= config.getWorkHoursPenalty();
                factors.add("Horario laboral");
            } else if (hour >= 6 && hour <= 8) {
                multiplier *= config.getDawnPatrolMultiplier();
            } else if (hour >= 18 && hour <= 20) {
                multiplier *= config.getAfterWorkMultiplier();
                factors.add("After-work");
            }
        }

        // Hora del día
        if (hour >= 10 && hour <= 14) {
            multiplier *= config.getMidDayPeakMultiplier();
        }

        // Estacionalidad
        double seasonality = calculateSeasonality(month, f);
        multiplier *= seasonality;
        if (seasonality > 1.0) {
            factors.add("Temporada alta");
        }

        return Math.min(config.getMaxTimeMultiplier(), multiplier);
    }

    /**
     * Calcula la calidad de las olas basándose en altura y periodo.
     */
    private double calculateWaveQuality(Spot spot, Forecast f, List<String> factors) {
        Double swellHeight = f.getSwellHeight();
        Double swellPeriod = f.getSwellPeriod();

        if (swellHeight == null) {
            factors.add("Sin datos de oleaje");
            return 0.5;
        }

        // Rangos óptimos (simplificado - usa "all levels" por defecto)
        double optimalMin = config.getAllLevelsSwellMin();
        double optimalMax = config.getAllLevelsSwellMax();
        double absoluteMax = config.getAllLevelsSwellAbsMax();

        double quality;
        if (swellHeight >= optimalMin && swellHeight <= optimalMax) {
            quality = 1.0;
            factors.add(String.format("Olas óptimas (%.1fm)", swellHeight));
        } else if (swellHeight < optimalMin) {
            quality = config.getTooSmallWaveQuality();
            factors.add("Olas pequeñas");
        } else if (swellHeight > absoluteMax) {
            quality = config.getCloseoutWaveQuality();
            factors.add("Olas muy grandes (closeout)");
        } else {
            // Zona de advertencia: entre óptimo y máximo absoluto
            double range = absoluteMax - optimalMax;
            double excess = swellHeight - optimalMax;
            quality = 1.0 - ((excess / range) * 0.6);
        }

        // Bonus/penalización por periodo
        if (swellPeriod != null) {
            if (swellPeriod > config.getLongPeriodThreshold()) {
                quality *= config.getLongPeriodBonus();
                factors.add("Groundswell");
            } else if (swellPeriod < config.getShortPeriodThreshold()) {
                quality *= config.getShortPeriodPenalty();
                factors.add("Windswell");
            }
        }

        return Math.max(0.0, Math.min(1.0, quality));
    }

    /**
     * Calcula la calidad del viento (offshore mejor que onshore).
     */
    private double calculateWindQuality(Spot spot, Forecast f, List<String> factors) {
        Double windSpeed = f.getWindSpeed();
        Double windDirection = f.getWindDirection();

        if (windSpeed == null) {
            return 0.5;
        }

        // Viento muy fuerte = insurfeable
        if (windSpeed > config.getStrongWindThreshold()) {
            factors.add("Viento fuerte");
            return 0.1;
        }

        // Glassy (sin viento)
        if (windSpeed < config.getLightWindThreshold()) {
            factors.add("Glassy");
            return config.getLightOffshoreScore();
        }

        // Calcular si es offshore u onshore
        double angleDiff = getAngleDifference(spot.getOptimalWindDirection(), windDirection);

        if (angleDiff <= config.getOffshoreAngleThreshold()) {
            factors.add("Offshore");
            return config.getLightOffshoreScore() * (1.0 - (windSpeed / 50.0));
        } else if (angleDiff >= config.getOnshoreAngleThreshold()) {
            factors.add("Onshore");
            return config.getLightOnshoreScore() * (1.0 - (windSpeed / 40.0));
        } else {
            return config.getSideshoreScore();
        }
    }

    /**
     * Calcula el factor de marea basándose en la preferencia del spot.
     */
    private double calculateTideFactor(Spot spot, Forecast f, List<String> factors) {
        if (f.getTideState() == null || spot.getOptimalTideState() == null) {
            return 1.0;
        }

        Spot.TidePreference pref = spot.getOptimalTideState();
        String current = f.getTideState();
        double sensitivity = spot.getTideSensitivity() != null ? spot.getTideSensitivity() : 0.5;

        double baseFactor = 1.0;

        switch (pref) {
            case HIGH:
                baseFactor = "HIGH".equals(current) ? config.getPerfectTideMultiplier()
                        : ("LOW".equals(current) ? config.getBadTideMultiplier() : 0.95);
                break;
            case LOW:
                baseFactor = "LOW".equals(current) ? config.getPerfectTideMultiplier()
                        : ("HIGH".equals(current) ? config.getBadTideMultiplier() : 0.95);
                break;
            case RISING:
            case MID_RISING:
                baseFactor = "RISING".equals(current) ? config.getPerfectTideMultiplier() : 0.9;
                break;
            case FALLING:
            case MID_FALLING:
                baseFactor = "FALLING".equals(current) ? config.getPerfectTideMultiplier() : 0.9;
                break;
            case ANY:
            default:
                baseFactor = 1.0;
        }

        // Aplicar sensibilidad
        if (baseFactor < 1.0) {
            double penalty = 1.0 - baseFactor;
            baseFactor = 1.0 - (penalty * sensitivity);
        }

        if (baseFactor > 1.0) {
            factors.add("Marea óptima");
        }

        return Math.max(0.5, Math.min(1.3, baseFactor));
    }

    /**
     * Calcula el factor climático (lluvia, temperatura).
     */
    private double calculateWeatherFactor(Forecast f, List<String> factors) {
        double multiplier = 1.0;

        // Lluvia
        if (Boolean.TRUE.equals(f.getIsRaining())) {
            multiplier *= config.getRainPenalty();
            factors.add("Lluvia");
        }

        // Temperatura
        Double temp = f.getTemperature();
        if (temp != null) {
            if (temp < 10.0) {
                multiplier *= config.getColdWaterPenalty();
                factors.add("Frío");
            } else if (temp > 25.0) {
                multiplier *= config.getWarmWaterBonus();
            }
        }

        return Math.max(0.5, Math.min(1.2, multiplier));
    }

    // ========================================================================
    // FUNCIONES AUXILIARES
    // ========================================================================

    /**
     * Función sigmoid que mapea calidad → multiplicador de crowd.
     * Cuando las condiciones son buenas, más gente sale a surfear.
     */
    private double calculateSigmoidStoke(double qualityScore) {
        double midpoint = config.getSigmoidMidpoint();
        double steepness = config.getSigmoidSteepness();
        double maxMult = config.getSigmoidMaxSaturation();

        double sigmoid = 1.0 / (1.0 + Math.exp(-steepness * (qualityScore - midpoint)));
        return 0.5 + (sigmoid * (maxMult - 0.5));
    }

    /**
     * Determina si es horario de luz diurna para surfear.
     */
    private boolean isDaylight(Forecast f) {
        LocalDateTime time = f.getTimestamp();
        LocalDateTime sunrise = f.getSunriseTime();
        LocalDateTime sunset = f.getSunsetTime();

        // Si no hay datos, asumir ventana típica 6-21h
        if (sunrise == null || sunset == null) {
            int hour = time.getHour();
            return hour >= 6 && hour <= 21;
        }

        // Ventana: desde amanecer hasta 1h después del atardecer
        LocalDateTime surfableEnd = sunset.plusHours(1);
        return !time.isBefore(sunrise) && !time.isAfter(surfableEnd);
    }

    /**
     * Calcula la confianza de la predicción (decae con el tiempo).
     */
    private double calculateConfidence(Forecast f) {
        long hoursOut = java.time.Duration.between(LocalDateTime.now(), f.getTimestamp()).toHours();

        if (hoursOut <= 0)
            return 0.85;
        if (hoursOut <= 12)
            return 0.80;
        if (hoursOut <= 24)
            return 0.70;
        if (hoursOut <= 48)
            return 0.55;
        return 0.40; // 48-72h
    }

    /**
     * Calcula la estacionalidad basándose en el mes y hemisferio.
     */
    private double calculateSeasonality(int month, Forecast f) {
        // Simplificado: verano = temporada alta
        boolean isSummer = (month >= 6 && month <= 9);
        boolean isWinter = (month >= 12 || month <= 2);

        if (isSummer)
            return config.getHighSeasonMultiplier();
        if (isWinter)
            return config.getLowSeasonMultiplier();
        return 1.0;
    }

    /**
     * Calcula la diferencia angular entre dos direcciones (0-180°).
     */
    private double getAngleDifference(Double target, Double current) {
        if (target == null || current == null)
            return 45.0;
        double diff = Math.abs(target - current) % 360;
        return diff > 180 ? 360 - diff : diff;
    }

    /**
     * Determina la categoría de crowd basándose en el score.
     */
    private String determineCategory(double score) {
        if (score < 2)
            return "EMPTY";
        if (score < 4)
            return "LOW";
        if (score < 7)
            return "MEDIUM";
        if (score < 9)
            return "HIGH";
        return "SATURATED";
    }

    /**
     * Construye predicción especial para horario nocturno (crowd = 0).
     */
    private CrowdPrediction buildNightPrediction(Spot spot, Forecast f) {
        return CrowdPrediction.builder()
                .spot(spot)
                .predictionTime(f.getTimestamp())
                .crowdLevel(0)
                .crowdCategory("EMPTY")
                .confidence(0.95)
                .conditionQuality(0.0)
                .rawScore(0.0)
                .tideHeight(f.getTideHeight())
                .tideState(f.getTideState())
                .primaryFactors("Horario nocturno")
                .build();
    }
}
