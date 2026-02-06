package com.crowdsurf.web;

import com.crowdsurf.domain.CrowdPrediction;
import com.crowdsurf.domain.CrowdReport;
import com.crowdsurf.domain.Spot;
import com.crowdsurf.service.PredictionService;
import com.crowdsurf.service.ReportService;
import com.crowdsurf.service.SpotService;
import com.crowdsurf.web.dto.PredictionResponse;
import com.crowdsurf.web.dto.ReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller principal de la API CrowdSurf.
 * 
 * Endpoints:
 * - GET /api/v1/spots → Lista todos los spots
 * - GET /api/v1/spots/{id}/prediction → Predicción actual + forecast 72h
 * - POST /api/v1/reports → Enviar reporte de crowd
 * - GET /api/v1/reports/recent → Reportes recientes
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "CrowdSurf API", description = "API de predicción de crowd en spots de surf")
@CrossOrigin(origins = "*")
public class CrowdController {

    private final SpotService spotService;
    private final PredictionService predictionService;
    private final ReportService reportService;

    // ========================================================================
    // SPOTS
    // ========================================================================

    @Operation(summary = "Lista todos los spots", description = "Retorna la lista completa de spots de surf disponibles")
    @GetMapping("/spots")
    public ResponseEntity<List<Spot>> getAllSpots() {
        return ResponseEntity.ok(spotService.getAllSpots());
    }

    // ========================================================================
    // PREDICCIONES
    // ========================================================================

    @Operation(summary = "Obtener predicción de crowd", description = "Retorna la predicción actual y forecast para las próximas horas")
    @GetMapping("/spots/{id}/prediction")
    public ResponseEntity<PredictionResponse> getPrediction(
            @Parameter(description = "ID del spot", example = "1") @PathVariable Long id,
            @Parameter(description = "Horas de forecast (1-72)", example = "48") @RequestParam(defaultValue = "48") int hours) {

        // Validar rango de horas
        int validHours = Math.max(1, Math.min(72, hours));

        // Obtener predicción actual
        CrowdPrediction current = predictionService.getCurrentPrediction(id);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }

        // Obtener forecast
        List<CrowdPrediction> forecastList = predictionService.getForecast(id, validHours);

        // Construir respuesta
        Spot spot = current.getSpot();
        PredictionResponse response = PredictionResponse.builder()
                .spot(PredictionResponse.SpotInfo.builder()
                        .id(spot.getId())
                        .name(spot.getName())
                        .latitude(spot.getLatitude())
                        .longitude(spot.getLongitude())
                        .basePopularity(spot.getBasePopularity())
                        .build())
                .current(mapToPredictionData(current))
                .forecast(forecastList.stream()
                        .map(this::mapToPredictionData)
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Mapea una entidad CrowdPrediction a PredictionData DTO.
     */
    private PredictionResponse.PredictionData mapToPredictionData(CrowdPrediction p) {
        return PredictionResponse.PredictionData.builder()
                .time(p.getPredictionTime())
                .crowdLevel(p.getCrowdLevel())
                .crowdCategory(p.getCrowdCategory())
                .confidence(p.getConfidence())
                .conditionQuality(p.getConditionQuality())
                .tideHeight(p.getTideHeight())
                .tideState(p.getTideState())
                .primaryFactors(p.getPrimaryFactors())
                .build();
    }

    // ========================================================================
    // REPORTES
    // ========================================================================

    @Operation(summary = "Enviar reporte de crowd", description = "Permite a usuarios reportar el nivel de crowd observado")
    @PostMapping("/reports")
    public ResponseEntity<Map<String, Long>> submitReport(
            @Valid @RequestBody ReportRequest request) {

        try {
            Long reportId = reportService.submitReport(request);
            return ResponseEntity.ok(Map.of("reportId", reportId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Obtener reportes recientes", description = "Retorna los 50 reportes más recientes")
    @GetMapping("/reports/recent")
    public ResponseEntity<List<CrowdReport>> getRecentReports() {
        return ResponseEntity.ok(reportService.getRecentReports());
    }

    // ========================================================================
    // HEALTH CHECK
    // ========================================================================

    @Operation(summary = "Health check", description = "Verificar estado de la API")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", "1.0.0",
                "spots", spotService.getAllSpots().size()));
    }
}
