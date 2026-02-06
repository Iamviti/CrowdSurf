package com.crowdsurf.provider;

import com.crowdsurf.domain.Forecast;
import com.crowdsurf.domain.Spot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Proveedor de datos meteorológicos usando la API gratuita Open-Meteo.
 * 
 * Combina dos APIs:
 * 1. Marine API: oleaje (altura, periodo, dirección), marea, temperatura del
 * agua
 * 2. Weather API: viento, lluvia, temperatura, amanecer/atardecer
 * 
 * Retorna pronósticos horarios para las próximas 48 horas.
 * Implementa retry con backoff exponencial para tolerancia a fallos.
 */
@Service
@Slf4j
public class OpenMeteoProvider {

    private final RestTemplate restTemplate;

    /** URL de la API Marine de Open-Meteo */
    private static final String MARINE_API_URL = "https://marine-api.open-meteo.com/v1/marine?" +
            "latitude={lat}&longitude={lon}" +
            "&hourly=wave_height,wave_direction,wave_period,sea_level_height_msl" +
            "&timezone=auto";

    /** URL de la API Weather de Open-Meteo */
    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast?" +
            "latitude={lat}&longitude={lon}" +
            "&hourly=temperature_2m,precipitation,wind_speed_10m,wind_direction_10m" +
            "&daily=sunset,sunrise" +
            "&timezone=auto";

    public OpenMeteoProvider() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Obtiene el pronóstico para las próximas 48 horas.
     * 
     * @param spot Spot con coordenadas
     * @return Lista de Forecast horarios
     */
    public List<Forecast> getForecast(Spot spot) {
        try {
            // 1. Fetch Marine Data
            OpenMeteoResponse marineResponse = fetchWithRetry(MARINE_API_URL, spot);

            // 2. Fetch Weather Data
            OpenMeteoResponse weatherResponse = null;
            try {
                weatherResponse = fetchWithRetry(WEATHER_API_URL, spot);
            } catch (Exception e) {
                log.warn("Weather API failed for spot {}, using marine only", spot.getName());
            }

            if (marineResponse == null || marineResponse.getHourly() == null) {
                return new ArrayList<>();
            }

            // 3. Combinar datos
            return combineData(spot, marineResponse, weatherResponse);

        } catch (Exception e) {
            log.error("Failed to fetch forecast for spot: {}", spot.getName(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Combina los datos de las APIs Marine y Weather en objetos Forecast.
     */
    private List<Forecast> combineData(Spot spot,
            OpenMeteoResponse marine,
            OpenMeteoResponse weather) {

        List<Forecast> forecasts = new ArrayList<>();
        OpenMeteoHourly marineData = marine.getHourly();
        OpenMeteoHourly weatherData = (weather != null) ? weather.getHourly() : new OpenMeteoHourly();

        String timezone = marine.getTimezone() != null ? marine.getTimezone() : "UTC";
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of(timezone));

        for (int i = 0; i < marineData.getTime().size(); i++) {
            LocalDateTime time = LocalDateTime.parse(marineData.getTime().get(i),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Filtrar: solo desde ahora hasta +48h
            if (time.isBefore(now.minusHours(1)))
                continue;
            if (time.isAfter(now.plusHours(48)))
                break;

            // Extraer datos seguros
            Double swellHeight = safeGet(marineData.getWaveHeight(), i);
            Double swellDirection = safeGet(marineData.getWaveDirection(), i);
            Double swellPeriod = safeGet(marineData.getWavePeriod(), i);
            Double tideHeight = safeGet(marineData.getSeaLevelHeight(), i);
            String tideState = calculateTideState(marineData.getSeaLevelHeight(), i);

            Double windSpeed = safeGet(weatherData.getWindSpeed10m(), i);
            Double windDirection = safeGet(weatherData.getWindDirection10m(), i);
            Double temperature = safeGet(weatherData.getTemperature2m(), i);
            Double precipitation = safeGet(weatherData.getPrecipitation(), i);

            // Sunrise/Sunset del día
            LocalDateTime sunrise = null;
            LocalDateTime sunset = null;
            if (weather != null && weather.getDaily() != null) {
                sunrise = findTimeForDate(weather.getDaily().getTime(),
                        weather.getDaily().getSunrise(), time.toLocalDate());
                sunset = findTimeForDate(weather.getDaily().getTime(),
                        weather.getDaily().getSunset(), time.toLocalDate());
            }

            Forecast forecast = Forecast.builder()
                    .timestamp(time)
                    .swellHeight(swellHeight)
                    .swellDirection(swellDirection)
                    .swellPeriod(swellPeriod)
                    .windSpeed(windSpeed != null ? windSpeed : 0.0)
                    .windDirection(windDirection != null ? windDirection : 0.0)
                    .tideHeight(tideHeight)
                    .tideState(tideState)
                    .temperature(temperature)
                    .precipitation(precipitation)
                    .isRaining(precipitation != null && precipitation > 0.0)
                    .sunriseTime(sunrise)
                    .sunsetTime(sunset)
                    .timezone(timezone)
                    .build();

            forecasts.add(forecast);
        }

        log.debug("Fetched {} forecast hours for spot: {}", forecasts.size(), spot.getName());
        return forecasts;
    }

    /**
     * Fetch con retry y backoff exponencial.
     */
    private OpenMeteoResponse fetchWithRetry(String url, Spot spot) throws Exception {
        int maxRetries = 3;
        long backoff = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return restTemplate.getForObject(url, OpenMeteoResponse.class,
                        spot.getLatitude(), spot.getLongitude());
            } catch (Exception e) {
                if (i == maxRetries - 1)
                    throw e;
                log.warn("API request failed for {} (attempt {}), retrying in {}ms...",
                        spot.getName(), i + 1, backoff);
                Thread.sleep(backoff);
                backoff *= 2;
            }
        }
        return null;
    }

    /**
     * Extracción segura de elemento de lista.
     */
    private Double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size())
            return null;
        return list.get(index);
    }

    /**
     * Calcula el estado de la marea basándose en la tendencia.
     */
    private String calculateTideState(List<Double> values, int index) {
        if (values == null || values.isEmpty())
            return null;

        Double current = safeGet(values, index);
        if (current == null)
            return null;

        Double prev = safeGet(values, index - 1);
        Double next = safeGet(values, index + 1);

        if (prev != null && next != null) {
            if (current > prev && current > next)
                return "HIGH";
            if (current < prev && current < next)
                return "LOW";
        }

        if (prev != null) {
            return (current >= prev) ? "RISING" : "FALLING";
        } else if (next != null) {
            return (next >= current) ? "RISING" : "FALLING";
        }

        return null;
    }

    /**
     * Busca la hora de sunrise/sunset para una fecha específica.
     */
    private LocalDateTime findTimeForDate(List<String> dates, List<String> values,
            java.time.LocalDate date) {
        if (dates == null || values == null)
            return null;

        for (int i = 0; i < dates.size(); i++) {
            try {
                java.time.LocalDate dailyDate = java.time.LocalDate.parse(dates.get(i));
                if (dailyDate.equals(date)) {
                    return LocalDateTime.parse(values.get(i), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            } catch (Exception e) {
                // Ignorar errores de parsing
            }
        }
        return null;
    }

    // ========================================================================
    // CLASES INTERNAS PARA DESERIALIZACIÓN JSON
    // ========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoResponse {
        private OpenMeteoHourly hourly;
        private OpenMeteoDaily daily;
        private String timezone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoDaily {
        private List<String> time;
        private List<String> sunset;
        private List<String> sunrise;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoHourly {
        private List<String> time = new ArrayList<>();

        @JsonProperty("wave_height")
        private List<Double> waveHeight = new ArrayList<>();

        @JsonProperty("wave_direction")
        private List<Double> waveDirection = new ArrayList<>();

        @JsonProperty("wave_period")
        private List<Double> wavePeriod = new ArrayList<>();

        @JsonProperty("sea_level_height_msl")
        private List<Double> seaLevelHeight = new ArrayList<>();

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m = new ArrayList<>();

        @JsonProperty("wind_direction_10m")
        private List<Double> windDirection10m = new ArrayList<>();

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m = new ArrayList<>();

        @JsonProperty("precipitation")
        private List<Double> precipitation = new ArrayList<>();
    }
}
