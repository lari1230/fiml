// movie.js - Функционал страницы фильма
class MovieManager {
    static movieId = null;
    static currentMovie = null;
    static userReview = null;

    static async init() {
        // Получаем ID фильма из URL
        const urlParams = new URLSearchParams(window.location.search);
        this.movieId = urlParams.get('id');

        if (!this.movieId) {
            this.showError('Фильм не найден');
            return;
        }

        // Загружаем информацию о фильме
        await this.loadMovie();

        // Загружаем отзывы
        await this.loadReviews();

        // Проверяем, оставлял ли пользователь отзыв
        await this.checkUserReview();

        // Показываем админские кнопки если нужно
        if (AuthManager.isAdmin()) {
            document.getElementById('adminActions').style.display = 'block';
        }

        // Инициализируем рейтинг кнопки
        this.initRatingButtons();
    }

    static async loadMovie() {
        try {
            const response = await ApiClient.get(`/api/movies/${this.movieId}`);

            if (response.success) {
                this.currentMovie = response.data;
                this.renderMovie();
                document.getElementById('loading').style.display = 'none';
                document.getElementById('movieContent').style.display = 'block';
            } else {
                this.showError('Фильм не найден');
            }
        } catch (error) {
            console.error('Load movie error:', error);
            this.showError('Ошибка загрузки фильма');
        }
    }

    static renderMovie() {
        const movie = this.currentMovie;

        // Заполняем основную информацию
        document.getElementById('movieTitle').textContent = movie.title;
        document.getElementById('moviePoster').src = movie.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster';
        document.getElementById('moviePoster').onerror = function() {
            this.src = 'https://via.placeholder.com/300x450?text=No+Poster';
        };
        document.getElementById('movieDirector').textContent = movie.director || 'Не указан';
        document.getElementById('movieYear').textContent = movie.year || 'Не указан';
        document.getElementById('movieDuration').textContent = movie.duration || 'Не указана';
        document.getElementById('movieDescription').textContent = movie.description || 'Описание отсутствует';

        // Рейтинг
        const rating = movie.averageRating || 0;
        document.getElementById('movieRating').textContent = rating.toFixed(1);

        // Звезды рейтинга
        const starsContainer = document.getElementById('movieStars');
        starsContainer.innerHTML = '';
        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('i');
            star.className = i <= Math.round(rating / 2) ? 'fas fa-star' : 'far fa-star';
            starsContainer.appendChild(star);
        }

        document.getElementById('reviewCount').textContent =
            `(${movie.reviews ? movie.reviews.length : 0} отзывов)`;

        // Жанры
        const genresContainer = document.getElementById('movieGenres');
        if (movie.genres && movie.genres.length > 0) {
            genresContainer.innerHTML = movie.genres.map(genre =>
                `<span class="genre-tag">${genre.name}</span>`
            ).join('');
        } else {
            genresContainer.innerHTML = '<span class="genre-tag">Жанры не указаны</span>';
        }

        // Кнопка отзыва
        const addReviewBtn = document.getElementById('addReviewBtn');
        if (AuthManager.isLoggedIn() && !this.userReview) {
            addReviewBtn.style.display = 'block';
        } else {
            addReviewBtn.style.display = 'none';
        }
    }

    static async loadReviews() {
        try {
            const response = await ApiClient.get(`/api/reviews/movie/${this.movieId}`);

            if (response.success) {
                this.renderReviews(response.data.reviews);
            }
        } catch (error) {
            console.error('Load reviews error:', error);
        }
    }

    static renderReviews(reviews) {
        const container = document.getElementById('reviewsContainer');

        if (!reviews || reviews.length === 0) {
            document.getElementById('noReviews').style.display = 'block';
            container.style.display = 'none';
            return;
        }

        document.getElementById('noReviews').style.display = 'none';
        container.style.display = 'block';

        container.innerHTML = reviews.map(review => `
            <div class="review-item" data-id="${review.id}">
                <div class="review-header">
                    <span class="review-user">${review.username}</span>
                    <span class="review-date">${Utils.formatDate(review.createdAt)}</span>
                    <div class="review-rating">
                        <i class="fas fa-star"></i> ${review.rating}/10
                    </div>
                </div>
                <p class="review-text">${Utils.escapeHtml(review.comment || '')}</p>
                ${review.userId === AuthManager.getCurrentUser()?.id ? `
                    <div class="review-actions">
                        <button class="btn btn-small" onclick="MovieManager.editReview(${review.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-small btn-danger" onclick="MovieManager.deleteReview(${review.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    static async checkUserReview() {
        if (!AuthManager.isLoggedIn()) return;

        try {
            const userReviews = await ReviewManager.getMyReviews();
            this.userReview = userReviews.find(review => review.movieId == this.movieId);

            if (this.userReview) {
                this.showExistingReview();
            }
        } catch (error) {
            console.error('Check user review error:', error);
        }
    }

    static showExistingReview() {
        const container = document.getElementById('existingReview');
        const review = this.userReview;

        document.getElementById('userRating').textContent = review.rating;
        document.getElementById('userComment').textContent = review.comment || 'Без комментария';
        document.getElementById('reviewDate').textContent = Utils.formatDate(review.createdAt);

        container.style.display = 'block';
        document.getElementById('addReviewBtn').style.display = 'none';
    }

    static initRatingButtons() {
        const container = document.getElementById('ratingButtons');
        container.innerHTML = '';

        for (let i = 1; i <= 10; i++) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'rating-btn';
            button.textContent = i;
            button.onclick = () => this.selectRating(i);
            container.appendChild(button);
        }
    }

    static selectRating(rating) {
        const buttons = document.querySelectorAll('.rating-btn');
        buttons.forEach(btn => {
            btn.classList.toggle('active', parseInt(btn.textContent) === rating);
        });
        document.getElementById('reviewRating').value = rating;
    }

    static showReviewForm() {
        if (!AuthManager.isLoggedIn()) {
            Utils.showNotification('Для оставления отзыва необходимо войти в систему', 'error');
            window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
            return;
        }

        document.getElementById('reviewForm').style.display = 'block';

        // Если есть существующий отзыв, заполняем форму
        if (this.userReview) {
            this.selectRating(this.userReview.rating);
            document.getElementById('reviewComment').value = this.userReview.comment || '';
        }
    }

    static hideReviewForm() {
        document.getElementById('reviewForm').style.display = 'none';
        document.getElementById('reviewFormContent').reset();
        this.initRatingButtons();
    }

    static async submitReview(event) {
        event.preventDefault();

        if (!AuthManager.isLoggedIn()) {
            Utils.showNotification('Необходима авторизация', 'error');
            return;
        }

        const rating = document.getElementById('reviewRating').value;
        const comment = document.getElementById('reviewComment').value.trim();

        if (!rating) {
            Utils.showNotification('Выберите оценку', 'error');
            return;
        }

        try {
            let success;

            if (this.userReview) {
                // Обновление существующего отзыва
                success = await ReviewManager.updateReview(
                    this.userReview.id,
                    parseInt(rating),
                    comment
                );
            } else {
                // Создание нового отзыва
                success = await ReviewManager.createReview(
                    parseInt(this.movieId),
                    parseInt(rating),
                    comment
                );
            }

            if (success) {
                Utils.showNotification('Отзыв сохранен', 'success');
                this.hideReviewForm();
                await Promise.all([
                    this.loadMovie(),
                    this.loadReviews(),
                    this.checkUserReview()
                ]);
            }
        } catch (error) {
            console.error('Submit review error:', error);
            Utils.showNotification('Ошибка сохранения отзыва', 'error');
        }
    }

    static editExistingReview() {
        if (this.userReview) {
            this.showReviewForm();
        }
    }

    static async deleteExistingReview() {
        if (!confirm('Удалить ваш отзыв?')) return;

        try {
            const success = await ReviewManager.deleteReview(this.userReview.id);
            if (success) {
                Utils.showNotification('Отзыв удален', 'success');
                this.userReview = null;
                document.getElementById('existingReview').style.display = 'none';
                document.getElementById('addReviewBtn').style.display = 'block';
                await Promise.all([
                    this.loadMovie(),
                    this.loadReviews()
                ]);
            }
        } catch (error) {
            console.error('Delete review error:', error);
            Utils.showNotification('Ошибка удаления отзыва', 'error');
        }
    }

    // Админские функции
    static editMovie() {
        if (!AuthManager.isAdmin()) return;

        const movie = this.currentMovie;
        document.getElementById('editMovieId').value = movie.id;
        document.getElementById('editMovieTitle').value = movie.title;
        document.getElementById('editMovieDirector').value = movie.director || '';
        document.getElementById('editMovieYear').value = movie.year || '';
        document.getElementById('editMovieDuration').value = movie.duration || '';
        document.getElementById('editMovieDescription').value = movie.description || '';
        document.getElementById('editMoviePosterUrl').value = movie.posterUrl || '';

        document.getElementById('editMovieModal').style.display = 'flex';
    }

    static closeEditMovieForm() {
        document.getElementById('editMovieModal').style.display = 'none';
        document.getElementById('editMovieForm').reset();
    }

    static async updateMovie(event) {
        event.preventDefault();

        if (!AuthManager.isAdmin()) return;

        const formData = {
            title: document.getElementById('editMovieTitle').value,
            director: document.getElementById('editMovieDirector').value,
            year: parseInt(document.getElementById('editMovieYear').value),
            description: document.getElementById('editMovieDescription').value,
            duration: parseInt(document.getElementById('editMovieDuration').value) || null,
            posterUrl: document.getElementById('editMoviePosterUrl').value || null
        };

        try {
            const response = await ApiClient.put(`/api/movies/${this.movieId}`, formData);

            if (response.success) {
                Utils.showNotification('Фильм обновлен', 'success');
                this.closeEditMovieForm();
                await this.loadMovie(); // Перезагружаем данные
            } else {
                Utils.showNotification(response.error || 'Ошибка обновления', 'error');
            }
        } catch (error) {
            console.error('Update movie error:', error);
            Utils.showNotification('Ошибка обновления фильма', 'error');
        }
    }

    static async deleteMovie() {
        if (!AuthManager.isAdmin()) return;

        if (!confirm('Удалить фильм и все связанные отзывы?')) return;

        try {
            const response = await ApiClient.delete(`/api/movies/${this.movieId}`);

            if (response.success) {
                Utils.showNotification('Фильм удален', 'success');
                setTimeout(() => {
                    window.location.href = '/catalog.html';
                }, 1000);
            } else {
                Utils.showNotification(response.error || 'Ошибка удаления', 'error');
            }
        } catch (error) {
            console.error('Delete movie error:', error);
            Utils.showNotification('Ошибка удаления фильма', 'error');
        }
    }

    static showError(message) {
        document.getElementById('loading').style.display = 'none';
        document.getElementById('errorContent').style.display = 'block';
        document.getElementById('errorContent').querySelector('h2').textContent = message;
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await MovieManager.init();
});