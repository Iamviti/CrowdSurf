/**
 * CrowdSurf Landing Page - JavaScript
 * Consumo de la API y visualización de predicciones
 */

const API_BASE = '/api/v1';

// DOM Elements
const spotSelect = document.getElementById('spotSelect');
const searchBtn = document.getElementById('searchBtn');
const predictionCard = document.getElementById('predictionCard');
const emptyState = document.getElementById('emptyState');

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
    loadSpots();
    setupEventListeners();
});

function setupEventListeners() {
    searchBtn.addEventListener('click', handleSearch);
    spotSelect.addEventListener('change', () => {
        if (spotSelect.value) {
            handleSearch();
        }
    });
}

// ============================================================================
// API CALLS
// ============================================================================

async function loadSpots() {
    try {
        const response = await fetch(`${API_BASE}/spots`);
        if (!response.ok) throw new Error('Failed to load spots');

        const spots = await response.json();
        populateSpotSelect(spots);
    } catch (error) {
        console.error('Error loading spots:', error);
        spotSelect.innerHTML = '<option value="">Error al cargar spots</option>';
    }
}

async function fetchPrediction(spotId) {
    try {
        const response = await fetch(`${API_BASE}/spots/${spotId}/prediction?hours=24`);
        if (!response.ok) throw new Error('Failed to fetch prediction');

        return await response.json();
    } catch (error) {
        console.error('Error fetching prediction:', error);
        return null;
    }
}

// ============================================================================
// UI HANDLERS
// ============================================================================

function populateSpotSelect(spots) {
    spotSelect.innerHTML = '<option value="">Selecciona un spot...</option>';

    spots.forEach(spot => {
        const option = document.createElement('option');
        option.value = spot.id;
        option.textContent = spot.name;
        spotSelect.appendChild(option);
    });
}

async function handleSearch() {
    const spotId = spotSelect.value;
    if (!spotId) return;

    // Show loading state
    searchBtn.textContent = '⏳ Cargando...';
    searchBtn.disabled = true;

    const data = await fetchPrediction(spotId);

    // Reset button
    searchBtn.textContent = '🔍 Ver Predicción';
    searchBtn.disabled = false;

    if (data) {
        displayPrediction(data);
    } else {
        showError();
    }
}

function displayPrediction(data) {
    emptyState.style.display = 'none';
    predictionCard.classList.remove('hidden');

    const { spot, current, forecast } = data;

    // Spot info
    document.getElementById('spotName').textContent = spot.name;
    document.getElementById('spotLocation').textContent =
        `${spot.latitude?.toFixed(2)}°, ${spot.longitude?.toFixed(2)}°`;

    // Crowd badge
    const crowdBadge = document.getElementById('crowdBadge');
    document.getElementById('crowdLevel').textContent = current.crowdLevel;
    crowdBadge.className = `crowd-badge ${getCrowdClass(current.crowdCategory)}`;

    // Current conditions
    document.getElementById('crowdValue').textContent = `${current.crowdLevel}/10`;
    document.getElementById('crowdCategory').textContent = translateCategory(current.crowdCategory);
    document.getElementById('confidence').textContent = `${Math.round((current.confidence || 0) * 100)}%`;
    document.getElementById('tideState').textContent = current.tideState || '-';

    // Factors
    document.getElementById('primaryFactors').textContent =
        current.primaryFactors || 'No hay factores disponibles';

    // Forecast chart
    if (forecast && forecast.length > 0) {
        renderForecastChart(forecast);
    }
}

function renderForecastChart(forecast) {
    const container = document.getElementById('forecastChart');
    container.innerHTML = '';

    // Take first 24 hours
    const hours = forecast.slice(0, 24);

    hours.forEach((item, index) => {
        const bar = document.createElement('div');
        bar.className = 'forecast-bar';

        const level = item.crowdLevel || 0;
        const height = (level / 10) * 100;
        const color = getCrowdColor(level);

        const hour = new Date(item.time).getHours();

        bar.innerHTML = `
            <span class="bar-value">${level}</span>
            <div class="bar-container">
                <div class="bar-fill" style="height: ${height}%; background: ${color};"></div>
            </div>
            <span class="bar-label">${hour}h</span>
        `;

        container.appendChild(bar);
    });
}

function showError() {
    emptyState.innerHTML = `
        <div class="empty-icon">⚠️</div>
        <p>Error al cargar predicción. Intenta de nuevo.</p>
    `;
    emptyState.style.display = 'block';
    predictionCard.classList.add('hidden');
}

// ============================================================================
// UTILITIES
// ============================================================================

function getCrowdClass(category) {
    const map = {
        'EMPTY': 'empty',
        'LOW': 'low',
        'MEDIUM': 'medium',
        'HIGH': 'high',
        'SATURATED': 'saturated'
    };
    return map[category] || 'medium';
}

function getCrowdColor(level) {
    if (level <= 2) return '#22c55e';      // Green - Empty
    if (level <= 4) return '#84cc16';      // Light green - Low
    if (level <= 6) return '#eab308';      // Yellow - Medium
    if (level <= 8) return '#f97316';      // Orange - High
    return '#ef4444';                       // Red - Saturated
}

function translateCategory(category) {
    const map = {
        'EMPTY': 'Vacío',
        'LOW': 'Bajo',
        'MEDIUM': 'Medio',
        'HIGH': 'Alto',
        'SATURATED': 'Saturado'
    };
    return map[category] || category;
}
