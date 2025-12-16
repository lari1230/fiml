// top.js - Функционал страницы топ фильмов
class TopMoviesManager {
    static async init() {
        // Загружаем топ фильмов
        await this.loadTopMovies();

        // Загружаем топ по жанрам
        await this.loadGenresTop();

        // Загружаем самые обсуждаемые
        await this.loadMostDiscussed();

        // Загружаем статистику
        await this.loadStats();

        // Загружаем список жанров для фильтра
        await this.loadGenresForFilter();
    }

    static async loadTopMovies() {
        try {
            const params = this.buildQueryParams();
            const response = await ApiClient.get(`/api/movies/top?${params}`);

            if (response.success) {
                this.renderTopMovies(response.data);
            } else {
                this.showError('Не удалось загрузить топ фильмов');
            }
        } catch (error) {
            console.error('Load top movies error:', error);
            this.showError('Ошибка загрузки топ фильмов');
        }
    }

    static buildQueryParams() {
        const params = new URLSearchParams();

        const timePeriod = document.getElementById('timePeriod').value;
        const minVotes = document.getElementById('minVotes').value;
        const genreFilter = document.getElementById('genreFilter').value;

        if (timePeriod !== 'all') params.append('period', timePeriod);
        if (minVotes) params.append('minVotes', minVotes);
        if (genreFilter) params.append('genre', genreFilter);

        params.append('limit', '20');

        return params.toString();
    }

    static renderTopMovies(movies) {
        const container = document.getElementById('topMoviesContainer');

        if (!movies || movies.length === 0) {
            container.innerHTML = `
                <div class="no-content">
                    <i class="fas fa-trophy"></i>
                    <h3>Фильмы не найдены</h3>
                    <p>Попробуйте изменить параметры фильтрации</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="top-list">
                ${movies.map((movie, index) => `
                    <div class="top-movie-item">
                        <div class="top-movie-rank">${index + 1}</div>
                        <div class="top-movie-poster">
                            <img src="${movie.posterUrl || 'https://via.placeholder.com/60x90?text=No+Poster'}"
                                 alt="${movie.title}"
                                 onerror="this.src='https://via.placeholder.com/60x90?text=No+Poster'">
                        </div>
                        <div class="top-movie-info">
                            <h4>
                                <a href="/movie.html?id=${movie.id}">${movie.title}</a>
                            </h4>
                            <p class="top-movie-meta">
                                ${movie.year || ''} ${movie.director ? `• ${movie.director}` : ''}
                            </p>
                        </div>
                        <div class="top-movie-rating">
                            ${movie.averageRating ? movie.averageRating.toFixed(1) : 'Н/Д'}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    static async loadGenresTop() {
        try {
            // В реальном приложении здесь запрос к API для топов по жанрам
            // Пока используем статические данные
            const genres = ['Драма', 'Комедия', 'Боевик', 'Фантастика', 'Триллер'];

            const container = document.getElementById('genresTop');
            container.innerHTML = genres.map(genre => `
                <div class="genre-top-card">
                    <h3>
                        <i class="fas fa-tag"></i>
                        ${genre}
                    </h3>
                    <div class="genre-top-list">
                        <div class="top-movie-item">
                            <div class="top-movie-rank">1</div>
                            <div class="top-movie-info">
                                <h4>Лучший фильм в жанре ${genre}</h4>
                            </div>
                            <div class="top-movie-rating">8.5</div>
                        </div>
                        <!-- Добавьте больше фильмов при необходимости -->
                    </div>
                </div>
            `).join('');
        } catch (error) {
            console.error('Load genres top error:', error);
        }
    }

    static async loadMostDiscussed() {
        try {
            // В реальном приложении здесь запрос к API для самых обсуждаемых фильмов
            const response = await ApiClient.get('/api/movies?sortBy=reviews&order=desc&limit=4');

            if (response.success) {
                this.renderMostDiscussed(response.data);
            }
        } catch (error) {
            console.error('Load most discussed error:', error);
        }
    }

    static renderMostDiscussed(movies) {
        const container = document.getElementById('mostDiscussed');

        if (!movies || movies.length === 0) {
            container.innerHTML = `
                <div class="no-content">
                    <i class="fas fa-comments"></i>
                    <p>Нет данных</p>
                </div>
            `;
            return;
        }

        container.innerHTML = movies.map(movie => `
            <div class="movie-card">
                <div class="movie-poster">
                    <img src="${movie.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster'}"
                         alt="${movie.title}"
                         onerror="this.src='https://via.placeholder.com/300x450?text=No+Poster'">
                    <div class="movie-discussion-badge">
                        <i class="fas fa-comment"></i>
                        ${movie.reviewCount || 0}
                    </div>
                </div>
                <div class="movie-info">
                    <h3 class="movie-title">${movie.title}</h3>
                    <p class="movie-director">${movie.director || 'Режиссер не указан'}</p>
                    <p class="movie-year">${movie.year || 'Год не указан'}</p>
                    <a href="/movie.html?id=${movie.id}" class="btn btn-small">
                        <i class="fas fa-info-circle"></i>
                        Подробнее
                    </a>
                </div>
            </div>
        `).join('');
    }

    static async loadStats() {
        try {
            // В реальном приложении здесь запрос к API для статистики
            // Пока используем статические данные
            document.getElementById('totalRatings').textContent = '1250';
            document.getElementById('ratingUsers').textContent = '450';
            document.getElementById('ratedMovies').textContent = '320';
            document.getElementById('averageAll').textContent = '7.2';
        } catch (error) {
            console.error('Load stats error:', error);
        }
    }

    static async loadGenresForFilter() {
        try {
            const response = await ApiClient.get('/api/genres');
            if (response.success) {
                const select = document.getElementById('genreFilter');
                select.innerHTML = `
                    <option value="">Все жанры</option>
                    ${response.data.map(genre =>
                        `<option value="${genre.id}">${genre.name}</option>`
                    ).join('')}
                `;
            }
        } catch (error) {
            console.error('Load genres for filter error:', error);
        }
    }

    static async loadTopMovies() {
        // Перезагружаем топ фильмов при изменении фильтров
        await this.loadTopMovies();
    }

    static showError(message) {
        const container = document.getElementById('topMoviesContainer');
        container.innerHTML = `
            <div class="error-content">
                <i class="fas fa-exclamation-triangle"></i>
                <h3>${message}</h3>
                <button class="btn btn-primary" onclick="TopMoviesManager.loadTopMovies()">
                    <i class="fas fa-redo"></i>
                    Попробовать снова
                </button>
            </div>
        `;
    }
}

// Глобальные функции
function loadTopMovies() {
    TopMoviesManager.loadTopMovies();
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await TopMoviesManager.init();
});