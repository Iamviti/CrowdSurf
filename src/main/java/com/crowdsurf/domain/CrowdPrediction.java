package com.crowdsurf.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entidad que representa una predicción de crowd para un spot en un momento
 * dado.
 * 
 * Resultado del motor heurístico que contiene:
 * - Nivel de crowd (0-10)
 * - Categoría descriptiva
 * - Confianza de la predicción
 * - Factores que influyeron
 * 
 * Se persiste en base de datos para consulta rápida y análisis histórico.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "crowd_predictions", indexes = {
        @Index(name = "idx_prediction_spot_time", columnList = "spot_id, predictionTime")
})
public class CrowdPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Spot al que pertenece la predicción */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    /** Momento de la predicción (hora local del spot) */
    private LocalDateTime predictionTime;

    /**
     * Nivel de crowd predicho (0-10).
     * 0 = vacío, 10 = saturado
     */
    private Integer crowdLevel;

    /**
     * Categoría descriptiva del crowd.
     * EMPTY, LOW, MEDIUM, HIGH, SATURATED
     */
    private String crowdCategory;

    /**
     * Confianza de la predicción (0.0-1.0).
     * Disminuye con el tiempo y aumenta con reportes reales.
     */
    private Double confidence;

    /**
     * Calidad de las condiciones de surf (0.0-1.0).
     * Combina oleaje, viento y marea.
     */
    private Double conditionQuality;

    /** Altura de marea en el momento de la predicción */
    private Double tideHeight;

    /** Estado de la marea (RISING, FALLING, HIGH, LOW) */
    private String tideState;

    /**
     * Factores principales que influyeron en la predicción.
     * Ej: "Weekend, Good Swell, Offshore Wind"
     */
    private String primaryFactors;

    /** Score sin normalizar (para debugging) */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Double rawScore;
}
