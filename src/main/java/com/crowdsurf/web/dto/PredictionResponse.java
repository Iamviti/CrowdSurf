package com.crowdsurf.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para predicciones de crowd.
 * 
 * Contiene la predicción actual y forecast para las próximas horas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** Información del spot */
    private SpotInfo spot;

    /** Predicción actual */
    private PredictionData current;

    /** Forecast para las próximas horas */
    private List<PredictionData> forecast;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private Double basePopularity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionData implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /** Hora de la predicción (local del spot) */
        private LocalDateTime time;

        /** Nivel de crowd (0-10) */
        private Integer crowdLevel;

        /** Categoría: EMPTY, LOW, MEDIUM, HIGH, SATURATED */
        private String crowdCategory;

        /** Confianza de la predicción (0.0-1.0) */
        private Double confidence;

        /** Calidad de las condiciones (0.0-1.0) */
        private Double conditionQuality;

        /** Altura de marea */
        private Double tideHeight;

        /** Estado de marea */
        private String tideState;

        /** Factores principales */
        private String primaryFactors;
    }
}
