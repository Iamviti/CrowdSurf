package com.crowdsurf.repository;

import com.crowdsurf.domain.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Spot.
 */
@Repository
public interface SpotRepository extends JpaRepository<Spot, Long> {

    /**
     * Busca un spot por nombre (case-insensitive).
     */
    Optional<Spot> findByNameIgnoreCase(String name);
}
