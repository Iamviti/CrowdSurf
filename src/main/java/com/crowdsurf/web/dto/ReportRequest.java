package com.crowdsurf.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO de request para envío de reportes de crowd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    /** ID del spot (requerido) */
    @NotNull(message = "Spot ID es requerido")
    private Long spotId;

    /** Nivel de crowd observado (0-10, requerido) */
    @NotNull(message = "Crowd Level es requerido")
    @Min(value = 0, message = "Crowd Level debe estar entre 0 y 10")
    @Max(value = 10, message = "Crowd Level debe estar entre 0 y 10")
    private Integer crowdLevel;

    /** ID del reportero (opcional) */
    private String reporterId;

    /** Notas adicionales (opcional) */
    private String notes;
}
