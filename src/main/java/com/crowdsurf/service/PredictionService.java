package com.crowdsurf.service;

import com.crowdsurf.domain.CrowdPrediction;
import com.crowdsurf.domain.Forecast;
import com.crowdsurf.domain.Spot;
import com.crowdsurf.engine.CrowdPredictor;
import com.crowdsurf.provider.OpenMeteoProvider;
import com.crowdsurf.repository.PredictionRepository;
import com.crowdsurf.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de predicción de crowd.
 * 
 * Orquesta:
 * 1. Obtención de datos meteorológicos (OpenMeteo)
 * 2. Cálculo de predicciones (CrowdPredictor)
 * 3. Almacenamiento en base de datos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final SpotRepository spotRepository;
    private final PredictionRepository predictionRepository;
    private final OpenMeteoProvider weatherProvider;
    private final CrowdPredictor crowdPredictor;

    /**
     * Obtiene la predicción actual para un spot.
     * Si no existe, genera una nueva.
     */
    @Transactional
    public CrowdPrediction getCurrentPrediction(Long spotId) {
        Spot spot = spotRepository.findById(spotId).orElse(null);
        if (spot == null)
            return null;

        LocalDateTime now = getSpotLocalTime(spot);

        CrowdPrediction prediction = predictionRepository
                .findTopBySpotIdAndPredictionTimeGreaterThanEqualOrderByPredictionTimeAsc(spotId, now);

        if (prediction != null) {
            return prediction;
        }

        // No hay predicción en cache, generar on-demand
        return generatePrediction(spot);
    }

    /**
     * Obtiene el forecast de predicciones para las próximas horas.
     */
    @Transactional
    public List<CrowdPrediction> getForecast(Long spotId, int hours) {
        Spot spot = spotRepository.findById(spotId).orElse(null);
        if (spot == null)
            return new ArrayList<>();

        LocalDateTime now = getSpotLocalTime(spot);
        LocalDateTime end = now.plusHours(hours);

        List<CrowdPrediction> predictions = predictionRepository
                .findBySpotIdAndPredictionTimeBetweenOrderByPredictionTimeAsc(spotId, now, end);

        // Si no hay suficientes predicciones, generar
        if (predictions.size() < hours / 2) {
            generatePredictions(spot);
            predictions = predictionRepository
                    .findBySpotIdAndPredictionTimeBetweenOrderByPredictionTimeAsc(spotId, now, end);
        }

        return predictions;
    }

    /**
     * Genera predicciones para un spot basándose en el forecast meteorológico.
     */
    @Transactional
    public CrowdPrediction generatePrediction(Spot spot) {
        List<Forecast> forecasts = weatherProvider.getForecast(spot);
        if (forecasts.isEmpty()) {
            log.warn("No forecast data for spot: {}", spot.getName());
            return null;
        }

        List<CrowdPrediction> predictions = generatePredictionsFromForecasts(spot, forecasts);
        if (predictions.isEmpty()) {
            return null;
        }

        predictionRepository.saveAll(predictions);
        return predictions.get(0);
    }

    /**
     * Genera predicciones para un spot (método interno).
     */
    @Transactional
    public void generatePredictions(Spot spot) {
        List<Forecast> forecasts = weatherProvider.getForecast(spot);
        if (forecasts.isEmpty()) {
            log.warn("No forecast data for spot: {}", spot.getName());
            return;
        }

        List<CrowdPrediction> predictions = generatePredictionsFromForecasts(spot, forecasts);
        if (!predictions.isEmpty()) {
            predictionRepository.saveAll(predictions);
            log.info("Generated {} predictions for spot: {}", predictions.size(), spot.getName());
        }
    }

    /**
     * Genera predicciones a partir de una lista de forecasts.
     */
    private List<CrowdPrediction> generatePredictionsFromForecasts(Spot spot, List<Forecast> forecasts) {
        return forecasts.stream()
                .map(forecast -> crowdPredictor.predict(spot, forecast))
                .collect(Collectors.toList());
    }

    /**
     * Job programado: regenera predicciones cada 6 horas.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void scheduledIngestion() {
        log.info("Starting scheduled prediction ingestion...");
        List<Spot> spots = spotRepository.findAll();

        for (Spot spot : spots) {
            try {
                generatePredictions(spot);
            } catch (Exception e) {
                log.error("Failed to generate predictions for spot: {}", spot.getName(), e);
            }
        }

        log.info("Finished prediction ingestion for {} spots", spots.size());
    }

    /**
     * Obtiene la hora local del spot.
     */
    private LocalDateTime getSpotLocalTime(Spot spot) {
        if (spot.getTimezone() != null) {
            try {
                return LocalDateTime.now(ZoneId.of(spot.getTimezone()));
            } catch (Exception e) {
                // Timezone inválida, usar UTC
            }
        }
        return LocalDateTime.now();
    }
}
