// main.js - Базовые функции и API клиент для киносайта

// ==================== API КЛИЕНТ ====================
class ApiClient {
    static async request(endpoint, options = {}) {
        const defaultOptions = {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const config = { ...defaultOptions, ...options };

        try {
            const response = await fetch(endpoint, config);

            // Если ответ не JSON (например, 404 для HTML страницы)
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                throw new Error(`Expected JSON response, got ${contentType}`);
            }

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || `HTTP error! status: ${response.status}`);
            }

            return data;
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    static async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    static async post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    static async put(endpoint, data) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    static async delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }

    static async patch(endpoint, data) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }
}

// ==================== МЕНЕДЖЕР АУТЕНТИФИКАЦИИ ====================
class AuthManager {
    static async checkAuth() {
        try {
            const response = await ApiClient.get('/api/auth/me');
            if (response.success) {
                localStorage.setItem('user', JSON.stringify(response.data));
                return response.data;
            }
            return null;
        } catch (error) {
            console.error('Auth check failed:', error);
            return null;
        }
    }

    static async login(email, password) {
        try {
            const response = await ApiClient.post('/api/auth/login', {
                email,
                password
            });

            if (response.success) {
                localStorage.setItem('user', JSON.stringify(response.data.user));
                Utils.showNotification('Вход выполнен успешно!', 'success');
                return response.data.user;
            } else {
                Utils.showNotification(response.error || 'Ошибка входа', 'error');
                return null;
            }
        } catch (error) {
            console.error('Login failed:', error);
            Utils.showNotification('Ошибка при входе: ' + error.message, 'error');
            return null;
        }
    }

    static async register(username, email, password) {
        try {
            const response = await ApiClient.post('/api/auth/register', {
                username,
                email,
                password
            });

            if (response.success) {
                localStorage.setItem('user', JSON.stringify(response.data.user));
                Utils.showNotification('Регистрация успешна!', 'success');
                return response.data.user;
            } else {
                Utils.showNotification(response.error || 'Ошибка регистрации', 'error');
                return null;
            }
        } catch (error) {
            console.error('Registration failed:', error);
            Utils.showNotification('Ошибка при регистрации: ' + error.message, 'error');
            return null;
        }
    }

    static async logout() {
        try {
            const response = await ApiClient.post('/api/auth/logout', {});
            localStorage.removeItem('user');
            Utils.showNotification('Вы успешно вышли', 'success');
            return response.success;
        } catch (error) {
            console.error('Logout failed:', error);
            Utils.showNotification('Ошибка при выходе', 'error');
            return false;
        }
    }

    static getCurrentUser() {
        const userJson = localStorage.getItem('user');
        return userJson ? JSON.parse(userJson) : null;
    }

    static isLoggedIn() {
        return this.getCurrentUser() !== null;
    }

    static isAdmin() {
        const user = this.getCurrentUser();
        return user && user.role === 'ADMIN';
    }

    static async updateProfile(userData) {
        try {
            const response = await ApiClient.put('/api/user/profile', userData);
            if (response.success) {
                // Обновляем данные пользователя
                const currentUser = this.getCurrentUser();
                const updatedUser = { ...currentUser, ...userData };
                localStorage.setItem('user', JSON.stringify(updatedUser));
                Utils.showNotification('Профиль обновлен', 'success');
                return true;
            } else {
                Utils.showNotification(response.error || 'Ошибка обновления', 'error');
                return false;
            }
        } catch (error) {
            console.error('Update profile failed:', error);
            Utils.showNotification('Ошибка обновления профиля', 'error');
            return false;
        }
    }

    static async changePassword(oldPassword, newPassword) {
        try {
            const response = await ApiClient.put('/api/user/password', {
                oldPassword,
                newPassword
            });

            if (response.success) {
                Utils.showNotification('Пароль успешно изменен', 'success');
                return true;
            } else {
                Utils.showNotification(response.error || 'Ошибка изменения пароля', 'error');
                return false;
            }
        } catch (error) {
            console.error('Change password failed:', error);
            Utils.showNotification('Ошибка изменения пароля', 'error');
            return false;
        }
    }
}

// ==================== УПРАВЛЕНИЕ ФИЛЬМАМИ ====================
class MovieManager {
    static async getMovies(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const endpoint = `/api/movies${queryString ? '?' + queryString : ''}`;

        try {
            const response = await ApiClient.get(endpoint);
            return response.success ? response.data : [];
        } catch (error) {
            console.error('Get movies failed:', error);
            return [];
        }
    }

    static async getMovie(id) {
        try {
            const response = await ApiClient.get(`/api/movies/${id}`);
            return response.success ? response.data : null;
        } catch (error) {
            console.error('Get movie failed:', error);
            return null;
        }
    }

    static async searchMovies(query) {
        try {
            const response = await ApiClient.get(`/api/movies/search?q=${encodeURIComponent(query)}`);
            return response.success ? response.data : [];
        } catch (error) {
            console.error('Search movies failed:', error);
            return [];
        }
    }

    static async getTopMovies(limit = 10) {
        try {
            const response = await ApiClient.get(`/api/movies/top?limit=${limit}`);
            return response.success ? response.data : [];
        } catch (error) {
            console.error('Get top movies failed:', error);
            return [];
        }
    }

    static async createMovie(movieData) {
        try {
            const response = await ApiClient.post('/api/movies', movieData);
            return response.success ? response.data : null;
        } catch (error) {
            console.error('Create movie failed:', error);
            return null;
        }
    }

    static async updateMovie(movieId, movieData) {
        try {
            const response = await ApiClient.put(`/api/movies/${movieId}`, movieData);
            return response.success;
        } catch (error) {
            console.error('Update movie failed:', error);
            return false;
        }
    }

    static async deleteMovie(movieId) {
        try {
            const response = await ApiClient.delete(`/api/movies/${movieId}`);
            return response.success;
        } catch (error) {
            console.error('Delete movie failed:', error);
            return false;
        }
    }
}

// ==================== УПРАВЛЕНИЕ ОТЗЫВАМИ ====================
class ReviewManager {
    static async getMovieReviews(movieId) {
        try {
            const response = await ApiClient.get(`/api/reviews/movie/${movieId}`);
            return response.success ? response.data : { reviews: [], averageRating: 0, reviewCount: 0 };
        } catch (error) {
            console.error('Get movie reviews failed:', error);
            return { reviews: [], averageRating: 0, reviewCount: 0 };
        }
    }

    static async createReview(movieId, rating, comment) {
        try {
            const response = await ApiClient.post('/api/reviews', {
                movieId,
                rating,
                comment
            });
            return response.success;
        } catch (error) {
            console.error('Create review failed:', error);
            return false;
        }
    }

    static async updateReview(reviewId, rating, comment) {
        try {
            const response = await ApiClient.put(`/api/reviews/${reviewId}`, {
                rating,
                comment
            });
            return response.success;
        } catch (error) {
            console.error('Update review failed:', error);
            return false;
        }
    }

    static async deleteReview(reviewId) {
        try {
            const response = await ApiClient.delete(`/api/reviews/${reviewId}`);
            return response.success;
        } catch (error) {
            console.error('Delete review failed:', error);
            return false;
        }
    }

    static async getMyReviews() {
        try {
            const response = await ApiClient.get('/api/reviews/my');
            return response.success ? response.data : [];
        } catch (error) {
            console.error('Get my reviews failed:', error);
            return [];
        }
    }
}

// ==================== УТИЛИТЫ ====================
class Utils {
    static showNotification(message, type = 'info') {
        // Удаляем старые уведомления
        const oldNotifications = document.querySelectorAll('.notification');
        oldNotifications.forEach(n => n.remove());

        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <span>${message}</span>
            <button onclick="this.parentElement.remove()">&times;</button>
        `;

        document.body.appendChild(notification);

        // Автоматическое удаление через 5 секунд
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }

    static formatDate(dateString) {
        if (!dateString) return '';

        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'только что';
        if (diffMins < 60) return `${diffMins} мин. назад`;
        if (diffHours < 24) return `${diffHours} ч. назад`;
        if (diffDays < 7) return `${diffDays} дн. назад`;

        return date.toLocaleDateString('ru-RU', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    static escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    static validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    static validatePassword(password) {
        return password.length >= 8;
    }

    static validateUsername(username) {
        return username.length >= 3 && username.length <= 20;
    }
}

// ==================== ГЛОБАЛЬНЫЕ ФУНКЦИИ ====================

// Обновление навигации
async function updateNavigation() {
    const authSection = document.getElementById('authSection');
    if (!authSection) return;

    const user = AuthManager.getCurrentUser() || await AuthManager.checkAuth();

    if (user) {
        authSection.innerHTML = `
            <div class="user-menu">
                <span class="username">${user.username}</span>
                <div class="dropdown">
                    <a href="/profile.html" class="dropdown-item">
                        <i class="fas fa-user"></i> Профиль
                    </a>
                    ${user.role === 'ADMIN' ? `
                        <a href="/admin.html" class="dropdown-item">
                            <i class="fas fa-cog"></i> Админ панель
                        </a>
                    ` : ''}
                    <button onclick="logout()" class="dropdown-item">
                        <i class="fas fa-sign-out-alt"></i> Выйти
                    </button>
                </div>
            </div>
        `;
    } else {
        authSection.innerHTML = `
            <a href="/login.html" class="btn btn-secondary btn-small">
                <i class="fas fa-sign-in-alt"></i>
                Войти
            </a>
            <a href="/register.html" class="btn btn-primary btn-small">
                <i class="fas fa-user-plus"></i>
                Регистрация
            </a>
        `;
    }
}

// Выход из системы
async function logout() {
    const success = await AuthManager.logout();
    if (success) {
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
    }
}

// Поиск фильмов
function searchMovies() {
    const searchInput = document.getElementById('globalSearch');
    if (searchInput) {
        const query = searchInput.value.trim();
        if (query) {
            window.location.href = `/catalog.html?q=${encodeURIComponent(query)}`;
        }
    }
}

// Переключение меню на мобильных устройствах
function toggleMenu() {
    const navMenu = document.getElementById('navMenu');
    if (navMenu) {
        navMenu.classList.toggle('show');
    }
}

// ==================== ИНИЦИАЛИЗАЦИЯ ПРИ ЗАГРУЗКЕ СТРАНИЦЫ ====================

document.addEventListener('DOMContentLoaded', async () => {
    await updateNavigation();

    // Проверяем, находится ли пользователь на странице, требующей авторизации
    const protectedPages = ['/profile.html', '/admin.html'];
    const currentPage = window.location.pathname;

    if (protectedPages.includes(currentPage) && !AuthManager.isLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    // Для админки проверяем права
    if (currentPage === '/admin.html' && !AuthManager.isAdmin()) {
        Utils.showNotification('Доступ запрещен. Требуются права администратора', 'error');
        window.location.href = '/';
        return;
    }

    // Проверяем авторизацию и загружаем данные пользователя
    const user = await AuthManager.checkAuth();
    if (user) {
        localStorage.setItem('user', JSON.stringify(user));

        // Показать кнопку добавления фильма для админов
        const addMovieBtn = document.getElementById('addMovieBtn');
        if (addMovieBtn && user.role === 'ADMIN') {
            addMovieBtn.style.display = 'block';
        }

        // Показать админ действия на странице фильма
        const adminActions = document.getElementById('adminActions');
        if (adminActions && user.role === 'ADMIN') {
            adminActions.style.display = 'block';
        }
    }

    // Добавляем обработку глобального поиска
    const globalSearch = document.getElementById('globalSearch');
    if (globalSearch) {
        globalSearch.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                searchMovies();
            }
        });
    }

    // Добавляем стили для уведомлений
    const notificationStyles = `
        .notification {
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 1rem 1.5rem;
            border-radius: 8px;
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
            min-width: 300px;
            max-width: 500px;
            z-index: 9999;
            animation: slideIn 0.3s ease;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        .notification-info {
            background-color: #3498db;
        }

        .notification-success {
            background-color: #2ecc71;
        }

        .notification-error {
            background-color: #e74c3c;
        }

        .notification-warning {
            background-color: #f39c12;
        }

        .notification button {
            background: none;
            border: none;
            color: white;
            font-size: 1.5rem;
            cursor: pointer;
            margin-left: 1rem;
            opacity: 0.8;
            padding: 0;
            width: 24px;
            height: 24px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .notification button:hover {
            opacity: 1;
        }

        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }

        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }
    `;

    // Вставляем стили в head
    if (!document.querySelector('#notification-styles')) {
        const styleSheet = document.createElement("style");
        styleSheet.id = 'notification-styles';
        styleSheet.textContent = notificationStyles;
        document.head.appendChild(styleSheet);
    }
});

// ==================== ГЛОБАЛЬНЫЕ ЭКСПОРТЫ ====================
// Делаем классы доступными глобально
window.ApiClient = ApiClient;
window.AuthManager = AuthManager;
window.MovieManager = MovieManager;
window.ReviewManager = ReviewManager;
window.Utils = Utils;

// Делаем функции доступными глобально
window.updateNavigation = updateNavigation;
window.logout = logout;
window.searchMovies = searchMovies;
window.toggleMenu = toggleMenu;