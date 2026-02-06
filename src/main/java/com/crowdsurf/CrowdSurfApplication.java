package com.crowdsurf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación CrowdSurf API.
 * 
 * API simplificada para predicción de crowd en spots de surf.
 * Endpoints principales:
 * - GET /api/v1/spots/{id}/prediction - Predicción de crowd (72h)
 * - POST /api/v1/reports - Envío de reportes de usuarios
 * 
 * @author CrowdSurf Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class CrowdSurfApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrowdSurfApplication.class, args);
    }
}
