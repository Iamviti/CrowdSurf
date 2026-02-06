package com.crowdsurf.service;

import com.crowdsurf.domain.CrowdPrediction;
import com.crowdsurf.domain.CrowdReport;
import com.crowdsurf.domain.Spot;
import com.crowdsurf.repository.PredictionRepository;
import com.crowdsurf.repository.ReportRepository;
import com.crowdsurf.repository.SpotRepository;
import com.crowdsurf.web.dto.ReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Servicio para gestión de reportes de crowd.
 * 
 * Implementa el "ground truth loop":
 * 1. Usuario reporta crowd observado
 * 2. Se compara con predicción del sistema
 * 3. El delta permite calibrar el modelo
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final SpotRepository spotRepository;
    private final PredictionRepository predictionRepository;

    /**
     * Procesa un reporte de crowd enviado por un usuario.
     * 
     * @param request DTO con spotId, crowdLevel y reporterId opcional
     * @return ID del reporte creado
     * @throws IllegalArgumentException si el spot no existe
     */
    @Transactional
    public Long submitReport(ReportRequest request) {
        Spot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new IllegalArgumentException("Spot not found: " + request.getSpotId()));

        LocalDateTime now = getSpotLocalTime(spot);

        // Construir reporte
        CrowdReport report = CrowdReport.builder()
                .spotId(spot.getId())
                .crowdLevel(request.getCrowdLevel())
                .observationTime(now)
                .source(CrowdReport.SourceType.USER)
                .reporterId(request.getReporterId() != null ? request.getReporterId() : "anonymous")
                .reliabilityScore(determineReliability(request.getReporterId()))
                .notes(request.getNotes())
                .build();

        // Ground Truth Loop: comparar con predicción del sistema
        CrowdPrediction systemPrediction = predictionRepository
                .findTopBySpotIdAndPredictionTimeGreaterThanEqualOrderByPredictionTimeAsc(spot.getId(), now);

        if (systemPrediction != null) {
            report.setSystemPredictedLevel(systemPrediction.getCrowdLevel());
            int delta = request.getCrowdLevel() - systemPrediction.getCrowdLevel();
            report.setPredictionDelta(delta);

            // Log desviaciones significativas
            if (Math.abs(delta) >= 3) {
                log.info("HIGH DEVIATION: Spot {} | Reported: {} | Predicted: {} | Delta: {}",
                        spot.getName(), request.getCrowdLevel(), systemPrediction.getCrowdLevel(), delta);
            }
        } else {
            log.warn("No prediction found for spot {} at report time", spot.getName());
        }

        CrowdReport saved = reportRepository.save(report);
        log.info("Report saved: id={}, spot={}, level={}", saved.getId(), spot.getName(), request.getCrowdLevel());

        return saved.getId();
    }

    /**
     * Obtiene los reportes más recientes.
     */
    @Transactional(readOnly = true)
    public List<CrowdReport> getRecentReports() {
        return reportRepository.findTop50ByOrderByObservationTimeDesc();
    }

    /**
     * Determina la fiabilidad del reportero.
     * Usuarios trusted/pro/admin tienen máxima fiabilidad.
     */
    private double determineReliability(String reporterId) {
        if (reporterId == null)
            return 0.5;
        String lower = reporterId.toLowerCase();
        if (lower.contains("admin") || lower.contains("pro") || lower.contains("trusted")) {
            return 1.0;
        }
        return 0.5;
    }

    /**
     * Obtiene la hora local del spot.
     */
    private LocalDateTime getSpotLocalTime(Spot spot) {
        if (spot.getTimezone() != null) {
            try {
                return LocalDateTime.now(ZoneId.of(spot.getTimezone()));
            } catch (Exception e) {
                // Timezone inválida
            }
        }
        return LocalDateTime.now();
    }
}
