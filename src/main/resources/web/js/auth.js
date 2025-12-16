// auth.js - Управление аутентификацией
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

// Инициализация при загрузке страницы
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
});

// Обновление навигации
async function updateNavigation() {
    const authSection = document.getElementById('authSection');
    if (!authSection) return;

    const user = AuthManager.getCurrentUser();

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

// Глобальные функции
async function logout() {
    const success = await AuthManager.logout();
    if (success) {
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
    }
}

function toggleMenu() {
    const navMenu = document.getElementById('navMenu');
    if (navMenu) {
        navMenu.classList.toggle('show');
    }
}

function searchMovies() {
    const searchInput = document.getElementById('globalSearch');
    if (searchInput) {
        const query = searchInput.value.trim();
        if (query) {
            window.location.href = `/catalog.html?q=${encodeURIComponent(query)}`;
        }
    }
}