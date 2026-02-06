package com.crowdsurf.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entidad que representa un reporte de crowd enviado por un usuario.
 * 
 * Forma parte del "ground truth loop" para calibrar el modelo:
 * - Usuarios reportan el crowd real observado
 * - Se compara con la predicción del sistema
 * - El delta permite ajustar la popularidad base del spot
 * 
 * Tipos de fuentes con diferente fiabilidad:
 * - PARTNER_INTEGRATION: Escuelas de surf (fiabilidad alta)
 * - TRUSTED_REPORTER: Local verificado (fiabilidad alta)
 * - USER: Usuario anónimo (fiabilidad media)
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "crowd_reports")
public class CrowdReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del spot reportado */
    @Column(nullable = false)
    private Long spotId;

    /** Momento de la observación */
    @Column(nullable = false)
    private LocalDateTime observationTime;

    /**
     * Nivel de crowd observado (0-10).
     * Reportado por el usuario.
     */
    @Column(nullable = false)
    private Integer crowdLevel;

    /** Tipo de fuente del reporte */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType source;

    /**
     * Puntuación de fiabilidad del reportero (0.0-1.0).
     * Partners = 1.0, usuarios nuevos = 0.5
     */
    private Double reliabilityScore;

    /** Nivel que el sistema predijo para ese momento */
    private Integer systemPredictedLevel;

    /**
     * Delta = Observado - Predicho.
     * Positivo = subestimamos, Negativo = sobreestimamos
     */
    private Integer predictionDelta;

    /** Identificador del reportero (opcional) */
    private String reporterId;

    /** Notas adicionales del usuario */
    private String notes;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum SourceType {
        /** Integración con escuela de surf o empresa */
        PARTNER_INTEGRATION,

        /** Local verificado de confianza */
        TRUSTED_REPORTER,

        /** Usuario genérico de la app */
        USER
    }
}
