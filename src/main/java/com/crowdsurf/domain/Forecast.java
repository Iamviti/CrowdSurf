package com.crowdsurf.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * DTO que representa las condiciones meteorológicas en un momento dado.
 * 
 * Contiene todos los datos necesarios para calcular la predicción de crowd:
 * - Oleaje (altura, periodo, dirección)
 * - Viento (velocidad, dirección)
 * - Marea (altura, estado)
 * - Clima (temperatura, lluvia)
 * - Astronomía (amanecer, atardecer)
 * 
 * Los datos se obtienen de la API Open-Meteo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Forecast {

    /** Timestamp del pronóstico (hora local del spot) */
    private LocalDateTime timestamp;

    // ========================================================================
    // OLEAJE (SWELL)
    // ========================================================================

    /** Altura del oleaje en metros */
    private Double swellHeight;

    /** Periodo del oleaje en segundos */
    private Double swellPeriod;

    /** Dirección del oleaje en grados (0-360) */
    private Double swellDirection;

    // ========================================================================
    // VIENTO
    // ========================================================================

    /** Velocidad del viento en km/h */
    private Double windSpeed;

    /** Dirección del viento en grados (0-360) */
    private Double windDirection;

    // ========================================================================
    // MAREA
    // ========================================================================

    /** Altura de la marea en metros */
    private Double tideHeight;

    /** Estado de la marea: RISING, FALLING, HIGH, LOW */
    private String tideState;

    // ========================================================================
    // CLIMA
    // ========================================================================

    /** Temperatura del aire en °C */
    private Double temperature;

    /** Temperatura del agua en °C */
    private Double waterTemperature;

    /** Precipitación en mm/h */
    private Double precipitation;

    /** Indica si está lloviendo */
    private Boolean isRaining;

    // ========================================================================
    // ASTRONOMÍA Y CONTEXTO
    // ========================================================================

    /** Hora del amanecer */
    private LocalDateTime sunriseTime;

    /** Hora del atardecer */
    private LocalDateTime sunsetTime;

    /** Zona horaria IANA (ej: "Europe/Madrid") */
    private String timezone;

    /** Indica si es festivo */
    private Boolean isHoliday;
}
