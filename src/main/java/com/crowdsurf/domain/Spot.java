package com.crowdsurf.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entidad que representa un spot de surf.
 * 
 * Contiene la información geográfica, características del spot,
 * y configuración heurística para el cálculo de crowd.
 * 
 * Los spots se cargan desde spots.csv al iniciar la aplicación.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "spots")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del spot (ej: "Mundaka", "Pipeline") */
    @Column(nullable = false)
    private String name;

    /** Latitud en grados decimales */
    private Double latitude;

    /** Longitud en grados decimales */
    private Double longitude;

    /**
     * Popularidad base del spot (0-10).
     * Representa cuánta gente suele haber históricamente.
     */
    private Double basePopularity;

    /**
     * Nivel de dificultad del spot.
     * Afecta los rangos óptimos de oleaje.
     */
    @Enumerated(EnumType.STRING)
    private SkillLevel skillLevel;

    /**
     * Accesibilidad física (1-10).
     * 10 = parking en la playa, 1 = requiere caminata.
     */
    private Integer accessibilityScore;

    /** Dirección óptima del swell (grados 0-360) */
    private Double optimalSwellDirection;

    /** Dirección óptima del viento - offshore (grados 0-360) */
    private Double optimalWindDirection;

    /**
     * Sensibilidad al viento (0.0-1.0).
     * 1.0 = muy sensible (se arruina con poco viento)
     */
    private Double windSensitivity;

    /** Estado de marea preferido */
    @Enumerated(EnumType.STRING)
    private TidePreference optimalTideState;

    /**
     * Sensibilidad a la marea (0.0-1.0).
     * 1.0 = solo funciona en marea específica (ej: Mundaka)
     */
    private Double tideSensitivity;

    /**
     * Dirección hacia el mar desde la playa (grados 0-360).
     * Se usa para calcular offshore/onshore automáticamente.
     */
    @Column(name = "beach_facing_direction")
    private Double beachFacingDirection;

    /** Zona horaria IANA (ej: "Europe/Madrid") */
    private String timezone;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum SkillLevel {
        BEGINNER, // Principiante - olas pequeñas y suaves
        INTERMEDIATE, // Intermedio - olas medianas
        EXPERT, // Experto - olas grandes y potentes
        ALL_LEVELS // Todos los niveles - variable
    }

    public enum TidePreference {
        HIGH, // Mejor en marea alta
        LOW, // Mejor en marea baja
        MID_RISING, // Mejor en media subiendo
        MID_FALLING, // Mejor en media bajando
        RISING, // Mejor cuando sube (cualquier altura)
        FALLING, // Mejor cuando baja (cualquier altura)
        ANY // Funciona con cualquier marea
    }

    // ========================================================================
    // MÉTODOS HELPER
    // ========================================================================

    /**
     * Obtiene la dirección óptima del swell.
     * Si no está definida, usa la dirección de la playa.
     */
    public Double getOptimalSwellDirection() {
        if (optimalSwellDirection != null)
            return optimalSwellDirection;
        if (beachFacingDirection != null)
            return beachFacingDirection;
        return 0.0;
    }

    /**
     * Obtiene la dirección óptima del viento (offshore).
     * Si no está definida, calcula el opuesto de la dirección de playa.
     */
    public Double getOptimalWindDirection() {
        if (optimalWindDirection != null)
            return optimalWindDirection;
        if (beachFacingDirection != null)
            return (beachFacingDirection + 180) % 360;
        return 0.0;
    }
}
