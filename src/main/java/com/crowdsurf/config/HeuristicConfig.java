package com.crowdsurf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración externalizada del motor heurístico de predicción de crowd.
 * 
 * Contiene los ~30 parámetros esenciales para calibrar el algoritmo.
 * Todos los valores pueden ajustarse en application.properties sin recompilar.
 * 
 * El modelo heurístico calcula:
 * Score = BasePopularity × StokeMultiplier × FactoresTemporales × FactoresClima
 * 
 * Donde StokeMultiplier usa una función sigmoid para mapear calidad de olas →
 * crowd.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.heuristics")
public class HeuristicConfig {

    // ========================================================================
    // PUNTUACIÓN BASE
    // ========================================================================

    /** Score máximo teórico que mapea a crowd level 10 */
    private double maxRawScore = 28.0;

    // ========================================================================
    // FACTORES TEMPORALES
    // ========================================================================

    /** Multiplicador para fines de semana y festivos (más gente) */
    private double weekendMultiplier = 1.4;

    /** Penalización durante horario laboral (9-17h días laborables) */
    private double workHoursPenalty = 0.6;

    /** Multiplicador dawn patrol (6-8h) */
    private double dawnPatrolMultiplier = 0.95;

    /** Multiplicador after-work (18-20h) */
    private double afterWorkMultiplier = 1.2;

    /** Multiplicador pico mediodía (10-14h) */
    private double midDayPeakMultiplier = 1.0;

    /** Límite máximo del multiplicador temporal */
    private double maxTimeMultiplier = 1.6;

    // ========================================================================
    // ESTACIONALIDAD
    // ========================================================================

    /** Multiplicador temporada alta (verano) */
    private double highSeasonMultiplier = 1.30;

    /** Multiplicador temporada baja (invierno) */
    private double lowSeasonMultiplier = 0.70;

    // ========================================================================
    // FACTORES CLIMÁTICOS
    // ========================================================================

    /** Penalización cuando llueve */
    private double rainPenalty = 0.85;

    /** Penalización agua fría (<10°C) */
    private double coldWaterPenalty = 0.85;

    /** Bonus agua cálida (>25°C) */
    private double warmWaterBonus = 1.05;

    // ========================================================================
    // CALIDAD DE OLAS - UMBRALES POR NIVEL
    // ========================================================================

    /** Altura mínima óptima para spots "all levels" */
    private double allLevelsSwellMin = 0.8;

    /** Altura máxima óptima para spots "all levels" */
    private double allLevelsSwellMax = 3.0;

    /** Altura máxima absoluta (closeout) */
    private double allLevelsSwellAbsMax = 4.0;

    /** Calidad cuando las olas son muy pequeñas */
    private double tooSmallWaveQuality = 0.5;

    /** Calidad cuando hay closeout (olas demasiado grandes) */
    private double closeoutWaveQuality = 0.3;

    // ========================================================================
    // PERIODO DE OLA
    // ========================================================================

    /** Umbral para considerarlo groundswell (periodo largo) */
    private double longPeriodThreshold = 12.0;

    /** Umbral para windswell (periodo corto) */
    private double shortPeriodThreshold = 8.0;

    /** Bonus por groundswell limpio */
    private double longPeriodBonus = 1.1;

    /** Penalización por windswell choppy */
    private double shortPeriodPenalty = 0.8;

    // ========================================================================
    // VIENTO
    // ========================================================================

    /** Umbral viento ligero - glassy (km/h) */
    private double lightWindThreshold = 6.0;

    /** Umbral viento fuerte (km/h) */
    private double strongWindThreshold = 30.0;

    /** Ángulo límite para considerar offshore */
    private double offshoreAngleThreshold = 45.0;

    /** Ángulo límite para considerar onshore */
    private double onshoreAngleThreshold = 135.0;

    /** Score viento offshore ligero (perfecto) */
    private double lightOffshoreScore = 1.0;

    /** Score viento onshore ligero (aceptable) */
    private double lightOnshoreScore = 0.5;

    /** Score viento lateral */
    private double sideshoreScore = 0.6;

    // ========================================================================
    // MAREA
    // ========================================================================

    /** Multiplicador marea perfecta */
    private double perfectTideMultiplier = 1.1;

    /** Multiplicador marea mala */
    private double badTideMultiplier = 0.9;

    // ========================================================================
    // MODELO SIGMOID "STOKE"
    // ========================================================================

    /** Punto medio de la curva sigmoid (calidad donde crowd acelera) */
    private double sigmoidMidpoint = 7.0;

    /** Pendiente de la curva sigmoid */
    private double sigmoidSteepness = 0.8;

    /** Multiplicador máximo de saturación */
    private double sigmoidMaxSaturation = 1.6;

    // ========================================================================
    // PESOS DE CALIDAD
    // ========================================================================

    /** Peso de calidad de olas en score efectivo */
    private double waveQualityWeight = 0.75;

    /** Peso de score de viento en score efectivo */
    private double windScoreWeight = 0.20;
}
