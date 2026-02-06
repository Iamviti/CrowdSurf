# CrowdSurf API 🌊

**API de predicción de crowd en spots de surf**

API simplificada que predice cuánta gente habrá en cualquier spot de surf del mundo, basándose en condiciones meteorológicas y patrones históricos.

## 🚀 Inicio Rápido

### Requisitos
- Java 17+
- Maven 3.6+
- PostgreSQL 15+ (o Docker)

### Opción 1: Con Docker
```bash
# Levantar API + Base de datos
docker-compose up --build
```

### Opción 2: Desarrollo Local
```bash
# 1. Iniciar PostgreSQL
docker run -d --name crowdsurf-db \
  -e POSTGRES_DB=crowdsurf \
  -e POSTGRES_PASSWORD=postgres123 \
  -p 5432:5432 postgres:15-alpine

# 2. Compilar y ejecutar
mvn clean package -DskipTests
java -jar target/crowdsurf-api-1.0.0.jar
```

## 📡 Endpoints

### Predicción de Crowd
```bash
GET /api/v1/spots/{id}/prediction?hours=48
```
Retorna predicción actual + forecast para las próximas horas.

**Respuesta:**
```json
{
  "spot": { "id": 3, "name": "Mundaka" },
  "current": {
    "crowdLevel": 6,
    "crowdCategory": "MEDIUM",
    "confidence": 0.75,
    "primaryFactors": "Fin de semana, Olas óptimas, Offshore"
  },
  "forecast": [...]
}
```

### Enviar Reporte
```bash
POST /api/v1/reports
```
Permite a usuarios reportar el crowd observado.

**Body:**
```json
{
  "spotId": 3,
  "crowdLevel": 7,
  "reporterId": "user123"
}
```

### Listar Spots
```bash
GET /api/v1/spots
```

### Health Check
```bash
GET /api/v1/health
```

## 🧮 Modelo Heurístico

El motor de predicción calcula:

```
Score = BasePopularity × StokeMultiplier × FactoresTemporales × FactoresClima
```

**Factores considerados:**
- 📊 **Oleaje**: altura, periodo, dirección
- 💨 **Viento**: velocidad, offshore/onshore
- 🌊 **Marea**: estado actual vs preferencia del spot
- 📅 **Tiempo**: día de semana, hora, estacionalidad
- ☀️ **Clima**: lluvia, temperatura

**Escala de Crowd (0-10):**
| Nivel | Categoría | Descripción |
|-------|-----------|-------------|
| 0-2 | EMPTY | Vacío |
| 2-4 | LOW | Poca gente |
| 4-7 | MEDIUM | Normal |
| 7-9 | HIGH | Crowded |
| 9-10 | SATURATED | Saturado |

## 📁 Estructura

```
CrowdSurf/
├── src/main/java/com/crowdsurf/
│   ├── config/         # Configuración heurística
│   ├── domain/         # Entidades JPA
│   ├── engine/         # Motor de predicción
│   ├── provider/       # Integración Open-Meteo
│   ├── repository/     # Repositorios JPA
│   ├── service/        # Lógica de negocio
│   └── web/            # Controllers y DTOs
├── src/main/resources/
│   ├── application.properties
│   └── spots.csv       # 50 spots precargados
├── Dockerfile
└── docker-compose.yml
```

## ⚙️ Configuración

Los parámetros heurísticos se configuran en `application.properties`:

```properties
# Multiplicador fin de semana
app.heuristics.weekendMultiplier=1.4

# Penalización lluvia
app.heuristics.rainPenalty=0.85

# Umbrales de oleaje
app.heuristics.allLevelsSwellMin=0.8
app.heuristics.allLevelsSwellMax=3.0
```

## 📖 Documentación API

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## 📝 Licencia

MIT License © 2026 CrowdSurf Team
