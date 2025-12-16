// catalog.js - Функционал страницы каталога
class CatalogManager {
    static currentPage = 1;
    static pageSize = 12;
    static totalPages = 1;
    static currentSearch = '';

    static async init() {
        // Получаем параметры из URL
        const urlParams = new URLSearchParams(window.location.search);
        const searchQuery = urlParams.get('q');

        if (searchQuery) {
            document.getElementById('searchInput').value = searchQuery;
            this.currentSearch = searchQuery;
            this.showSearchResults(searchQuery);
        }

        // Загружаем фильмы
        await this.loadMovies();

        // Показываем кнопку добавления фильма для админов
        if (AuthManager.isAdmin()) {
            document.getElementById('addMovieBtn').style.display = 'block';
        }

        // Загружаем список жанров для формы
        await this.loadGenresForForm();
    }

    static async loadMovies() {
        try {
            const params = this.buildQueryParams();
            const response = await ApiClient.get(`/api/movies?${params}`);

            if (response.success) {
                this.renderMovies(response.data);
                this.updatePagination();
            } else {
                this.showError('Не удалось загрузить фильмы');
            }
        } catch (error) {
            console.error('Load movies error:', error);
            this.showError('Ошибка загрузки фильмов');
        }
    }

    static buildQueryParams() {
        const params = new URLSearchParams();

        // Параметры пагинации
        params.append('page', this.currentPage);
        params.append('limit', this.pageSize);

        // Параметры сортировки
        const sortBy = document.getElementById('sortBy').value;
        const sortOrder = document.getElementById('sortOrder').value;

        if (sortBy) params.append('sortBy', sortBy);
        if (sortOrder) params.append('order', sortOrder);

        // Параметры фильтрации по году
        const yearFrom = document.getElementById('yearFrom').value;
        const yearTo = document.getElementById('yearTo').value;

        if (yearFrom) params.append('yearFrom', yearFrom);
        if (yearTo) params.append('yearTo', yearTo);

        // Поисковый запрос
        if (this.currentSearch) {
            params.append('q', this.currentSearch);
        }

        return params.toString();
    }

    static renderMovies(movies) {
        const container = document.getElementById('moviesContainer');

        if (!movies || movies.length === 0) {
            container.innerHTML = `
                <div class="no-content">
                    <i class="fas fa-film"></i>
                    <h3>Фильмы не найдены</h3>
                    <p>Попробуйте изменить параметры поиска</p>
                </div>
            `;
            return;
        }

        container.innerHTML = movies.map(movie => `
            <div class="movie-card" data-id="${movie.id}">
                <div class="movie-poster">
                    <img src="${movie.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster'}"
                         alt="${movie.title}"
                         onerror="this.src='https://via.placeholder.com/300x450?text=No+Poster'">
                    <div class="movie-rating">
                        <i class="fas fa-star"></i>
                        ${movie.averageRating ? movie.averageRating.toFixed(1) : 'Н/Д'}
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

    static updatePagination() {
        document.getElementById('currentPage').textContent = this.currentPage;
        document.getElementById('totalPages').textContent = this.totalPages;

        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');

        prevBtn.disabled = this.currentPage <= 1;
        nextBtn.disabled = this.currentPage >= this.totalPages;
    }

    static async performSearch() {
        const query = document.getElementById('searchInput').value.trim();

        if (query) {
            this.currentSearch = query;
            this.currentPage = 1;
            this.showSearchResults(query);
            await this.loadMovies();
        } else {
            this.clearSearch();
        }
    }

    static showSearchResults(query) {
        const resultsDiv = document.getElementById('searchResults');
        resultsDiv.style.display = 'block';
        resultsDiv.innerHTML = `
            <h3>Результаты поиска: "${query}"</h3>
            <button class="btn btn-small" onclick="CatalogManager.clearSearch()">
                <i class="fas fa-times"></i>
                Очистить поиск
            </button>
        `;
    }

    static clearSearch() {
        this.currentSearch = '';
        this.currentPage = 1;
        document.getElementById('searchInput').value = '';
        document.getElementById('searchResults').style.display = 'none';

        // Убираем параметр поиска из URL
        const url = new URL(window.location);
        url.searchParams.delete('q');
        window.history.replaceState({}, '', url);

        this.loadMovies();
    }

    static resetFilters() {
        document.getElementById('sortBy').value = 'created_at';
        document.getElementById('sortOrder').value = 'desc';
        document.getElementById('yearFrom').value = '';
        document.getElementById('yearTo').value = '';

        this.currentPage = 1;
        this.loadMovies();
    }

    static async loadGenresForForm() {
        try {
            const response = await ApiClient.get('/api/genres');
            if (response.success) {
                const select = document.getElementById('movieGenres');
                select.innerHTML = response.data.map(genre =>
                    `<option value="${genre.id}">${genre.name}</option>`
                ).join('');
            }
        } catch (error) {
            console.error('Load genres error:', error);
        }
    }

    static showAddMovieForm() {
        document.getElementById('addMovieModal').style.display = 'flex';
    }

    static closeAddMovieForm() {
        document.getElementById('addMovieModal').style.display = 'none';
        document.getElementById('addMovieForm').reset();
    }

    static async addMovie(event) {
        event.preventDefault();

        const formData = {
            title: document.getElementById('movieTitle').value,
            director: document.getElementById('movieDirector').value,
            year: parseInt(document.getElementById('movieYear').value),
            description: document.getElementById('movieDescription').value,
            duration: parseInt(document.getElementById('movieDuration').value) || null,
            posterUrl: document.getElementById('moviePosterUrl').value || null,
            genres: Array.from(document.getElementById('movieGenres').selectedOptions)
                .map(option => option.value)
        };

        try {
            const response = await ApiClient.post('/api/movies', formData);

            if (response.success) {
                Utils.showNotification('Фильм успешно добавлен', 'success');
                this.closeAddMovieForm();
                this.loadMovies(); // Перезагружаем список
            } else {
                Utils.showNotification(response.error || 'Ошибка добавления фильма', 'error');
            }
        } catch (error) {
            console.error('Add movie error:', error);
            Utils.showNotification('Ошибка добавления фильма', 'error');
        }
    }

    static showError(message) {
        const container = document.getElementById('moviesContainer');
        container.innerHTML = `
            <div class="error-message">
                <i class="fas fa-exclamation-triangle"></i>
                <p>${message}</p>
                <button class="btn btn-small" onclick="CatalogManager.loadMovies()">
                    <i class="fas fa-redo"></i>
                    Попробовать снова
                </button>
            </div>
        `;
    }
}

// Пагинация
function prevPage() {
    if (CatalogManager.currentPage > 1) {
        CatalogManager.currentPage--;
        CatalogManager.loadMovies();
    }
}

function nextPage() {
    if (CatalogManager.currentPage < CatalogManager.totalPages) {
        CatalogManager.currentPage++;
        CatalogManager.loadMovies();
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await CatalogManager.init();
});