// profile.js - Функционал личного кабинета
class ProfileManager {
    static currentTab = 'reviews';

    static async init() {
        // Проверяем авторизацию
        if (!AuthManager.isLoggedIn()) {
            document.getElementById('loading').style.display = 'none';
            document.getElementById('notAuthorized').style.display = 'block';
            return;
        }

        // Загружаем данные профиля
        await this.loadProfile();

        // Инициализируем вкладки
        this.initTabs();

        // Загружаем данные для активной вкладки
        await this.loadTabData();
    }

    static async loadProfile() {
        try {
            const user = AuthManager.getCurrentUser();

            // Заполняем основную информацию
            document.getElementById('profileUsername').textContent = user.username;
            document.getElementById('profileEmail').textContent = user.email;
            document.getElementById('profileRole').textContent = user.role === 'ADMIN' ? 'Администратор' : 'Пользователь';
            document.getElementById('profileJoined').textContent = `Зарегистрирован: ${Utils.formatDate(user.createdAt)}`;

            // Загружаем статистику
            await this.loadStats();

            document.getElementById('loading').style.display = 'none';
            document.getElementById('profileContent').style.display = 'block';
        } catch (error) {
            console.error('Load profile error:', error);
            this.showError('Ошибка загрузки профиля');
        }
    }

    static async loadStats() {
        try {
            const reviews = await ReviewManager.getMyReviews();

            document.getElementById('reviewsCount').textContent = reviews.length;
            document.getElementById('moviesRated').textContent = reviews.length;
            document.getElementById('commentsCount').textContent = reviews.filter(r => r.comment).length;

            // Расчет дней на сайте
            const user = AuthManager.getCurrentUser();
            const joinDate = new Date(user.createdAt);
            const today = new Date();
            const days = Math.floor((today - joinDate) / (1000 * 60 * 60 * 24));
            document.getElementById('daysActive').textContent = Math.max(1, days);
        } catch (error) {
            console.error('Load stats error:', error);
        }
    }

    static initTabs() {
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tabId = btn.getAttribute('onclick').match(/switchTab\('(\w+)'\)/)[1];
                this.switchTab(tabId);
            });
        });
    }

    static async switchTab(tabId) {
        // Обновляем активные кнопки
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`.tab-btn[onclick="switchTab('${tabId}')"]`).classList.add('active');

        // Скрываем все вкладки
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.remove('active');
        });

        // Показываем выбранную вкладку
        document.getElementById(`${tabId}Tab`).classList.add('active');

        // Сохраняем текущую вкладку
        this.currentTab = tabId;

        // Загружаем данные для вкладки
        await this.loadTabData();
    }

    static async loadTabData() {
        switch (this.currentTab) {
            case 'reviews':
                await this.loadUserReviews();
                break;
            case 'favorites':
                await this.loadFavorites();
                break;
            case 'watchlist':
                await this.loadWatchlist();
                break;
            // Для settings ничего не загружаем
        }
    }

    static async loadUserReviews() {
        try {
            const reviews = await ReviewManager.getMyReviews();
            const container = document.getElementById('userReviews');

            if (reviews.length === 0) {
                document.getElementById('noReviews').style.display = 'block';
                container.style.display = 'none';
                return;
            }

            document.getElementById('noReviews').style.display = 'none';
            container.style.display = 'block';

            container.innerHTML = reviews.map(review => `
                <div class="review-item">
                    <div class="review-header">
                        <a href="/movie.html?id=${review.movieId}" class="review-movie">
                            ${review.movieTitle || 'Фильм'}
                        </a>
                        <span class="review-date">${Utils.formatDate(review.createdAt)}</span>
                        <div class="review-rating">
                            <i class="fas fa-star"></i> ${review.rating}/10
                        </div>
                    </div>
                    <p class="review-text">${Utils.escapeHtml(review.comment || 'Без комментария')}</p>
                    <div class="review-actions">
                        <a href="/movie.html?id=${review.movieId}" class="btn btn-small">
                            <i class="fas fa-film"></i> К фильму
                        </a>
                        <button class="btn btn-small" onclick="ProfileManager.editReview(${review.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-small btn-danger" onclick="ProfileManager.deleteReview(${review.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            `).join('');
        } catch (error) {
            console.error('Load user reviews error:', error);
            Utils.showNotification('Ошибка загрузки отзывов', 'error');
        }
    }

    static async loadFavorites() {
        // В реальном приложении здесь загрузка избранных фильмов
        const container = document.getElementById('favoritesList');
        container.innerHTML = `
            <div class="no-content">
                <i class="fas fa-heart"></i>
                <h3>Список избранного пуст</h3>
                <p>Добавляйте фильмы в избранное, чтобы быстро находить их позже</p>
                <a href="/catalog.html" class="btn btn-primary">
                    <i class="fas fa-film"></i>
                    Перейти к каталогу
                </a>
            </div>
        `;
    }

    static async loadWatchlist() {
        // В реальном приложении здесь загрузка списка для просмотра
        const container = document.getElementById('watchlist');
        container.innerHTML = `
            <div class="no-content">
                <i class="fas fa-clock"></i>
                <h3>Список для просмотра пуст</h3>
                <p>Добавляйте фильмы, которые планируете посмотреть</p>
                <a href="/catalog.html" class="btn btn-primary">
                    <i class="fas fa-film"></i>
                    Перейти к каталогу
                </a>
            </div>
        `;
    }

    static showEditProfileForm() {
        const user = AuthManager.getCurrentUser();

        document.getElementById('editUsername').value = user.username;
        document.getElementById('editEmail').value = user.email;

        document.getElementById('editProfileModal').style.display = 'flex';
    }

    static closeEditProfileForm() {
        document.getElementById('editProfileModal').style.display = 'none';
        document.getElementById('editProfileForm').reset();
    }

    static async updateProfile(event) {
        event.preventDefault();

        const formData = {
            username: document.getElementById('editUsername').value,
            email: document.getElementById('editEmail').value
        };

        try {
            const success = await AuthManager.updateProfile(formData);
            if (success) {
                this.closeEditProfileForm();
                await this.loadProfile(); // Перезагружаем профиль
            }
        } catch (error) {
            console.error('Update profile error:', error);
        }
    }

    static showChangePasswordForm() {
        document.getElementById('changePasswordModal').style.display = 'flex';
    }

    static closeChangePasswordForm() {
        document.getElementById('changePasswordModal').style.display = 'none';
        document.getElementById('changePasswordForm').reset();
    }

    static async changePassword(event) {
        event.preventDefault();

        const oldPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmNewPassword').value;

        // Валидация
        if (newPassword !== confirmPassword) {
            Utils.showNotification('Пароли не совпадают', 'error');
            return;
        }

        if (newPassword.length < 8) {
            Utils.showNotification('Пароль должен содержать минимум 8 символов', 'error');
            return;
        }

        try {
            const success = await AuthManager.changePassword(oldPassword, newPassword);
            if (success) {
                this.closeChangePasswordForm();
            }
        } catch (error) {
            console.error('Change password error:', error);
        }
    }

    static async editReview(reviewId) {
        // Находим отзыв
        const reviews = await ReviewManager.getMyReviews();
        const review = reviews.find(r => r.id === reviewId);

        if (review) {
            // Перенаправляем на страницу фильма с открытой формой редактирования
            window.location.href = `/movie.html?id=${review.movieId}#edit-review`;
        }
    }

    static async deleteReview(reviewId) {
        if (!confirm('Удалить этот отзыв?')) return;

        try {
            const success = await ReviewManager.deleteReview(reviewId);
            if (success) {
                Utils.showNotification('Отзыв удален', 'success');
                await this.loadUserReviews(); // Перезагружаем список
                await this.loadStats(); // Обновляем статистику
            }
        } catch (error) {
            console.error('Delete review error:', error);
            Utils.showNotification('Ошибка удаления отзыва', 'error');
        }
    }

    static saveSettings() {
        const settings = {
            notifyReviews: document.getElementById('notifyReviews').checked,
            notifyNews: document.getElementById('notifyNews').checked,
            showProfile: document.getElementById('showProfile').checked,
            showReviews: document.getElementById('showReviews').checked
        };

        localStorage.setItem('userSettings', JSON.stringify(settings));
        Utils.showNotification('Настройки сохранены', 'success');

        // В реальном приложении здесь отправка на сервер
    }

    static loadSettings() {
        const saved = localStorage.getItem('userSettings');
        if (saved) {
            const settings = JSON.parse(saved);
            document.getElementById('notifyReviews').checked = settings.notifyReviews;
            document.getElementById('notifyNews').checked = settings.notifyNews;
            document.getElementById('showProfile').checked = settings.showProfile;
            document.getElementById('showReviews').checked = settings.showReviews;
        }
    }

    static showError(message) {
        const container = document.getElementById('profileContent');
        container.innerHTML = `
            <div class="error-content">
                <i class="fas fa-exclamation-triangle"></i>
                <h2>${message}</h2>
                <button class="btn btn-primary" onclick="location.reload()">
                    <i class="fas fa-redo"></i>
                    Попробовать снова
                </button>
            </div>
        `;
    }
}

// Глобальные функции
function switchTab(tabId) {
    ProfileManager.switchTab(tabId);
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await ProfileManager.init();
    ProfileManager.loadSettings();
});