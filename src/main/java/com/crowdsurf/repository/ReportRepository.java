package com.crowdsurf.repository;

import com.crowdsurf.domain.CrowdReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para reportes de crowd.
 */
@Repository
public interface ReportRepository extends JpaRepository<CrowdReport, Long> {

    /**
     * Busca los reportes más recientes.
     */
    List<CrowdReport> findTop50ByOrderByObservationTimeDesc();

    /**
     * Busca reportes de un spot en un período de tiempo.
     */
    List<CrowdReport> findBySpotIdAndObservationTimeBetween(
            Long spotId, LocalDateTime from, LocalDateTime to);
}
