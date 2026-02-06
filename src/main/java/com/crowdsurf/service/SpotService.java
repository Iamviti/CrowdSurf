package com.crowdsurf.service;

import com.crowdsurf.domain.Spot;
import com.crowdsurf.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Servicio para gestión de spots de surf.
 * 
 * Carga los spots desde spots.csv al iniciar la aplicación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpotService {

    private final SpotRepository spotRepository;

    /**
     * Carga los spots desde el archivo CSV al iniciar la aplicación.
     * Solo carga si la base de datos está vacía.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadSpotsFromCsv() {
        if (spotRepository.count() > 0) {
            log.info("Spots already loaded: {} spots in database", spotRepository.count());
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("spots.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            // Saltar header
            String header = reader.readLine();
            if (header == null) {
                log.warn("Empty spots.csv file");
                return;
            }

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    Spot spot = parseSpotFromCsv(line);
                    spotRepository.save(spot);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to parse spot line: {}", line, e);
                }
            }

            reader.close();
            log.info("Loaded {} spots from CSV", count);

        } catch (Exception e) {
            log.error("Failed to load spots from CSV", e);
        }
    }

    /**
     * Parsea una línea CSV a un objeto Spot.
     * Formato CSV (header):
     * name,latitude,longitude,beachFacingDirection,basePopularity,skillLevel,
     * optimalTideState,tideSensitivity,optimalSwellDirection,optimalWindDirection,
     * windSensitivity,accessibilityScore,timezone,predictionMode
     */
    private Spot parseSpotFromCsv(String line) {
        String[] parts = line.split(",", -1);

        Spot spot = new Spot();
        spot.setName(cleanCsvValue(parts[0])); // 0: name
        spot.setLatitude(parseDouble(parts[1])); // 1: latitude
        spot.setLongitude(parseDouble(parts[2])); // 2: longitude
        spot.setBeachFacingDirection(parseDouble(parts[3])); // 3: beachFacingDirection
        spot.setBasePopularity(parseDouble(parts[4])); // 4: basePopularity
        spot.setSkillLevel(parseSkillLevel(parts[5])); // 5: skillLevel
        spot.setOptimalTideState(parseTidePreference(parts[6])); // 6: optimalTideState
        spot.setTideSensitivity(parseDouble(parts[7])); // 7: tideSensitivity
        spot.setOptimalSwellDirection(parseDouble(parts[8])); // 8: optimalSwellDirection
        spot.setOptimalWindDirection(parseDouble(parts[9])); // 9: optimalWindDirection
        spot.setWindSensitivity(parts.length > 10 ? parseDouble(parts[10]) : 0.4); // 10: windSensitivity
        spot.setAccessibilityScore(parts.length > 11 ? parseInt(parts[11]) : 5); // 11: accessibilityScore
        spot.setTimezone(parts.length > 12 ? cleanCsvValue(parts[12]) : "UTC"); // 12: timezone
        // 13: predictionMode - no se usa en esta versión

        return spot;
    }

    private String cleanCsvValue(String value) {
        if (value == null)
            return null;
        return value.trim().replace("\"", "");
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Spot.SkillLevel parseSkillLevel(String value) {
        if (value == null || value.trim().isEmpty())
            return Spot.SkillLevel.ALL_LEVELS;
        try {
            return Spot.SkillLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Spot.SkillLevel.ALL_LEVELS;
        }
    }

    private Spot.TidePreference parseTidePreference(String value) {
        if (value == null || value.trim().isEmpty())
            return Spot.TidePreference.ANY;
        try {
            return Spot.TidePreference.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Spot.TidePreference.ANY;
        }
    }

    /**
     * Obtiene todos los spots.
     */
    public List<Spot> getAllSpots() {
        return spotRepository.findAll();
    }

    /**
     * Busca un spot por ID.
     */
    public Spot getSpotById(Long id) {
        return spotRepository.findById(id).orElse(null);
    }
}
