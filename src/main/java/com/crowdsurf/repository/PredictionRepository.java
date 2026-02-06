package com.crowdsurf.repository;

import com.crowdsurf.domain.CrowdPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para predicciones de crowd.
 */
@Repository
public interface PredictionRepository extends JpaRepository<CrowdPrediction, Long> {

    /**
     * Busca la predicción más cercana para un spot a partir de un momento.
     */
    CrowdPrediction findTopBySpotIdAndPredictionTimeGreaterThanEqualOrderByPredictionTimeAsc(
            Long spotId, LocalDateTime from);

    /**
     * Busca predicciones de un spot en un rango de tiempo.
     */
    List<CrowdPrediction> findBySpotIdAndPredictionTimeBetweenOrderByPredictionTimeAsc(
            Long spotId, LocalDateTime from, LocalDateTime to);

    /**
     * Elimina predicciones antiguas para un spot.
     */
    void deleteBySpotIdAndPredictionTimeBefore(Long spotId, LocalDateTime cutoff);
}
