// admin.js - Функционал админ панели
class AdminManager {
    static currentTab = 'dashboard';
    static usersPage = 1;
    static moviesPage = 1;
    static usersPageSize = 10;
    static moviesPageSize = 10;

    static async init() {
        // Проверяем права
        if (!AuthManager.isAdmin()) {
            document.getElementById('loading').style.display = 'none';
            document.getElementById('accessDenied').style.display = 'block';
            return;
        }

        // Инициализируем дашборд
        await this.loadDashboard();

        // Инициализируем вкладки
        this.initTabs();

        document.getElementById('loading').style.display = 'none';
        document.getElementById('adminContent').style.display = 'block';
    }

    static async loadDashboard() {
        try {
            const response = await ApiClient.get('/api/admin/dashboard');

            if (response.success) {
                this.renderDashboard(response.data);
            }
        } catch (error) {
            console.error('Load dashboard error:', error);
        }
    }

    static renderDashboard(data) {
        // Общая статистика
        document.getElementById('totalUsers').textContent = data.totalUsers;
        document.getElementById('totalMovies').textContent = data.totalMovies;
        document.getElementById('totalReviews').textContent = data.totalReviews;
        document.getElementById('pendingReviews').textContent = data.pendingReviews;

        // Сегодняшняя статистика
        document.getElementById('todayUsers').textContent = data.todayUsers;
        document.getElementById('todayReviews').textContent = data.todayReviews;
        document.getElementById('avgRating').textContent = data.averageRating;
        document.getElementById('activeUsers').textContent = data.activeUsers;

        // Топ фильмов
        this.renderTopMovies(data.topMovies || []);
    }

    static renderTopMovies(movies) {
        const container = document.querySelector('.activity-list');
        if (!container || !movies.length) return;

        const moviesHTML = movies.map((movie, index) => `
            <div class="activity-item">
                <i class="fas fa-film"></i>
                <div class="activity-content">
                    <div class="activity-text">
                        <strong>${index + 1}.</strong> ${movie.title} (${movie.averageRating}⭐)
                    </div>
                    <div class="activity-time">${movie.reviewCount} отзывов</div>
                </div>
            </div>
        `).join('');

        container.innerHTML = moviesHTML;
    }

    static initTabs() {
        const tabBtns = document.querySelectorAll('.admin-nav .tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tabId = btn.getAttribute('onclick').match(/switchAdminTab\('(\w+)'\)/)[1];
                this.switchAdminTab(tabId);
            });
        });
    }

    static async switchAdminTab(tabId) {
        // Обновляем активные кнопки
        document.querySelectorAll('.admin-nav .tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`.admin-nav .tab-btn[onclick="switchAdminTab('${tabId}')"]`).classList.add('active');

        // Скрываем все вкладки
        document.querySelectorAll('.admin-tab-pane').forEach(pane => {
            pane.classList.remove('active');
        });

        // Показываем выбранную вкладку
        document.getElementById(`${tabId}Tab`).classList.add('active');

        // Сохраняем текущую вкладку
        this.currentTab = tabId;

        // Загружаем данные для вкладки
        await this.loadTabData(tabId);
    }

    static async loadTabData(tabId) {
        switch (tabId) {
            case 'movies':
                await this.loadMovies();
                break;
            case 'reviews':
                await this.loadReviews();
                break;
            case 'users':
                await this.loadUsers();
                break;
            case 'genres':
                await this.loadGenres();
                break;
            case 'settings':
                // Настройки не требуют загрузки
                break;
        }
    }

    static async loadMovies() {
        try {
            const response = await ApiClient.get(`/api/admin/movies?page=${this.moviesPage}&limit=${this.moviesPageSize}`);

            if (response.success) {
                this.renderMovies(response.data.movies);
                this.updateMoviesPagination(response.data.pagination);
            }
        } catch (error) {
            console.error('Load admin movies error:', error);
            Utils.showNotification('Ошибка загрузки фильмов', 'error');
        }
    }

    static renderMovies(movies) {
        const tbody = document.getElementById('moviesTable');

        if (!movies || movies.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        <div class="no-content">
                            <i class="fas fa-film"></i>
                            <p>Фильмы не найдены</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = movies.map(movie => `
            <tr data-id="${movie.id}">
                <td>${movie.id}</td>
                <td>
                    ${movie.posterUrl ?
                        `<img src="${movie.posterUrl}" alt="${movie.title}" style="width: 50px; height: 75px; object-fit: cover;">` :
                        '<i class="fas fa-image" style="font-size: 24px; color: #ccc;"></i>'
                    }
                </td>
                <td>
                    <a href="/movie.html?id=${movie.id}" class="movie-link">
                        ${movie.title}
                    </a>
                </td>
                <td>${movie.director || '-'}</td>
                <td>${movie.year || '-'}</td>
                <td>
                    ${movie.averageRating ?
                        `<span class="rating-badge">${movie.averageRating.toFixed(1)}</span>` :
                        '<span class="rating-badge">Н/Д</span>'
                    }
                </td>
                <td>${movie.reviewCount || 0}</td>
                <td>
                    <div class="admin-actions">
                        <button class="btn btn-small edit-btn" onclick="AdminManager.editMovie(${movie.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-small delete-btn" onclick="AdminManager.deleteMovie(${movie.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    static updateMoviesPagination(pagination) {
        this.moviesPage = pagination.page;
        this.moviesPageSize = pagination.limit;
        this.totalMoviesPages = pagination.pages;

        document.getElementById('currentMoviesPage').textContent = pagination.page;
        document.getElementById('totalMoviesPages').textContent = pagination.pages;

        const prevBtn = document.getElementById('prevMoviesPage');
        const nextBtn = document.getElementById('nextMoviesPage');

        prevBtn.disabled = pagination.page <= 1;
        nextBtn.disabled = pagination.page >= pagination.pages;
    }

    static async loadReviews() {
        try {
            const filter = document.getElementById('reviewFilter').value;
            const response = await ApiClient.get(`/api/admin/reviews?filter=${filter}`);

            if (response.success) {
                this.renderReviews(response.data.reviews);
            }
        } catch (error) {
            console.error('Load admin reviews error:', error);
            Utils.showNotification('Ошибка загрузки отзывов', 'error');
        }
    }

    static renderReviews(reviews) {
        const tbody = document.getElementById('reviewsTable');

        if (!reviews || reviews.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        <div class="no-content">
                            <i class="fas fa-comment"></i>
                            <p>Отзывы не найдены</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = reviews.map(review => `
            <tr data-id="${review.id}">
                <td>${review.id}</td>
                <td>${review.username}</td>
                <td>
                    <a href="/movie.html?id=${review.movieId}" class="movie-link">
                        ${review.movieTitle}
                    </a>
                </td>
                <td>
                    <span class="rating-badge">${review.rating}/10</span>
                </td>
                <td>
                    <div class="review-comment-preview">
                        ${review.comment ? Utils.escapeHtml(review.comment.substring(0, 100)) + (review.comment.length > 100 ? '...' : '') : '-'}
                    </div>
                </td>
                <td>${Utils.formatDate(review.createdAt)}</td>
                <td>
                    ${review.isApproved ?
                        '<span class="status-badge status-approved">Одобрен</span>' :
                        '<span class="status-badge status-pending">На модерации</span>'
                    }
                </td>
                <td>
                    <div class="admin-actions">
                        ${!review.isApproved ? `
                            <button class="btn btn-small approve-btn" onclick="AdminManager.approveReview(${review.id})">
                                <i class="fas fa-check"></i>
                            </button>
                        ` : ''}
                        <button class="btn btn-small delete-btn" onclick="AdminManager.deleteAdminReview(${review.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    static async loadUsers() {
        try {
            const filter = document.getElementById('userFilter').value;
            const response = await ApiClient.get(`/api/admin/users?page=${this.usersPage}&limit=${this.usersPageSize}&filter=${filter}`);

            if (response.success) {
                this.renderUsers(response.data.users);
                this.updateUsersPagination(response.data.pagination);
            }
        } catch (error) {
            console.error('Load admin users error:', error);
            Utils.showNotification('Ошибка загрузки пользователей', 'error');
        }
    }

    static renderUsers(users) {
        const tbody = document.getElementById('usersTable');

        if (!users || users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        <div class="no-content">
                            <i class="fas fa-users"></i>
                            <p>Пользователи не найдены</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = users.map(user => `
            <tr data-id="${user.id}">
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email}</td>
                <td>
                    <span class="role-badge role-${user.role.toLowerCase()}">
                        ${user.role === 'ADMIN' ? 'Админ' : 'Пользователь'}
                    </span>
                </td>
                <td>${Utils.formatDate(user.createdAt)}</td>
                <td>
                    ${user.isActive ?
                        '<span class="status-badge status-active">Активен</span>' :
                        '<span class="status-badge status-inactive">Заблокирован</span>'
                    }
                </td>
                <td>${user.reviewCount || 0}</td>
                <td>
                    <div class="admin-actions">
                        <button class="btn btn-small edit-btn" onclick="AdminManager.editUser(${user.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        ${user.role !== 'ADMIN' ? `
                            <button class="btn btn-small ${user.isActive ? 'btn-warning' : 'btn-success'}"
                                    onclick="AdminManager.toggleUserStatus(${user.id}, ${!user.isActive})">
                                <i class="fas fa-${user.isActive ? 'ban' : 'check'}"></i>
                            </button>
                        ` : ''}
                        ${user.role !== 'ADMIN' ? `
                            <button class="btn btn-small delete-btn" onclick="AdminManager.deleteUser(${user.id})">
                                <i class="fas fa-trash"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `).join('');
    }

    static updateUsersPagination(pagination) {
        this.usersPage = pagination.page;
        this.usersPageSize = pagination.limit;
        this.totalUsersPages = pagination.pages;

        document.getElementById('currentUsersPage').textContent = pagination.page;
        document.getElementById('totalUsersPages').textContent = pagination.pages;

        const prevBtn = document.getElementById('prevUsersPage');
        const nextBtn = document.getElementById('nextUsersPage');

        prevBtn.disabled = pagination.page <= 1;
        nextBtn.disabled = pagination.page >= pagination.pages;
    }

    static async loadGenres() {
        try {
            const response = await ApiClient.get('/api/admin/genres');

            if (response.success) {
                this.renderGenres(response.data);
            }
        } catch (error) {
            console.error('Load genres error:', error);
            Utils.showNotification('Ошибка загрузки жанров', 'error');
        }
    }

    static renderGenres(genres) {
        const container = document.getElementById('genresGrid');

        if (!genres || genres.length === 0) {
            container.innerHTML = `
                <div class="no-content">
                    <i class="fas fa-tags"></i>
                    <p>Жанры не найдены</p>
                </div>
            `;
            return;
        }

        container.innerHTML = genres.map(genre => `
            <div class="genre-admin-card" data-id="${genre.id}">
                <div>
                    <h4>${genre.name}</h4>
                    <small>${genre.movieCount || 0} фильмов</small>
                </div>
                <div class="genre-actions">
                    <button class="btn btn-small edit-btn" onclick="AdminManager.editGenre(${genre.id}, '${genre.name}')">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-small delete-btn" onclick="AdminManager.deleteGenre(${genre.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `).join('');
    }

    // === Управление фильмами ===

    static showAddMovieModal() {
        document.getElementById('addMovieModal').style.display = 'flex';
    }

    static closeAddMovieModal() {
        document.getElementById('addMovieModal').style.display = 'none';
        document.getElementById('addMovieForm').reset();
    }

    static async addMovie(event) {
        event.preventDefault();

        const formData = {
            title: document.getElementById('adminMovieTitle').value,
            director: document.getElementById('adminMovieDirector').value,
            year: parseInt(document.getElementById('adminMovieYear').value),
            description: document.getElementById('adminMovieDescription').value,
            duration: parseInt(document.getElementById('adminMovieDuration').value) || null,
            posterUrl: document.getElementById('adminMoviePosterUrl').value || null,
            genres: Array.from(document.querySelectorAll('#adminGenresList input:checked'))
                .map(input => input.value)
        };

        try {
            const response = await ApiClient.post('/api/admin/movies', formData);

            if (response.success) {
                Utils.showNotification('Фильм успешно добавлен', 'success');
                this.closeAddMovieModal();
                await this.loadMovies();
            } else {
                Utils.showNotification(response.error || 'Ошибка добавления фильма', 'error');
            }
        } catch (error) {
            console.error('Add movie error:', error);
            Utils.showNotification('Ошибка добавления фильма', 'error');
        }
    }

    static async editMovie(movieId) {
        try {
            const response = await ApiClient.get(`/api/movies/${movieId}`);

            if (response.success) {
                const movie = response.data;

                document.getElementById('editMovieId').value = movie.id;
                document.getElementById('editAdminMovieTitle').value = movie.title;
                document.getElementById('editAdminMovieDirector').value = movie.director || '';
                document.getElementById('editAdminMovieYear').value = movie.year || '';
                document.getElementById('editAdminMovieDuration').value = movie.duration || '';
                document.getElementById('editAdminMovieDescription').value = movie.description || '';
                document.getElementById('editAdminMoviePosterUrl').value = movie.posterUrl || '';

                document.getElementById('editMovieModal').style.display = 'flex';
            }
        } catch (error) {
            console.error('Edit movie error:', error);
            Utils.showNotification('Ошибка загрузки данных фильма', 'error');
        }
    }

    static closeEditMovieModal() {
        document.getElementById('editMovieModal').style.display = 'none';
        document.getElementById('editMovieForm').reset();
    }

    static async updateAdminMovie(event) {
        event.preventDefault();

        const movieId = document.getElementById('editMovieId').value;
        const formData = {
            title: document.getElementById('editAdminMovieTitle').value,
            director: document.getElementById('editAdminMovieDirector').value,
            year: parseInt(document.getElementById('editAdminMovieYear').value),
            description: document.getElementById('editAdminMovieDescription').value,
            duration: parseInt(document.getElementById('editAdminMovieDuration').value) || null,
            posterUrl: document.getElementById('editAdminMoviePosterUrl').value || null
        };

        try {
            const response = await ApiClient.put(`/api/admin/movies/${movieId}`, formData);

            if (response.success) {
                Utils.showNotification('Фильм обновлен', 'success');
                this.closeEditMovieModal();
                await this.loadMovies();
            } else {
                Utils.showNotification(response.error || 'Ошибка обновления', 'error');
            }
        } catch (error) {
            console.error('Update movie error:', error);
            Utils.showNotification('Ошибка обновления фильма', 'error');
        }
    }

    static async deleteMovie(movieId) {
        if (!confirm('Удалить фильм и все связанные отзывы?')) return;

        try {
            const response = await ApiClient.delete(`/api/admin/movies/${movieId}`);

            if (response.success) {
                Utils.showNotification('Фильм удален', 'success');
                await this.loadMovies();
            } else {
                Utils.showNotification(response.error || 'Ошибка удаления', 'error');
            }
        } catch (error) {
            console.error('Delete movie error:', error);
            Utils.showNotification('Ошибка удаления фильма', 'error');
        }
    }

    // === Управление отзывами ===

    static filterReviews() {
        this.loadReviews();
    }

    static async approveReview(reviewId) {
        try {
            const response = await ApiClient.patch(`/api/admin/reviews/${reviewId}/approve`);

            if (response.success) {
                Utils.showNotification('Отзыв одобрен', 'success');
                await this.loadReviews();
            } else {
                Utils.showNotification(response.error || 'Ошибка одобрения', 'error');
            }
        } catch (error) {
            console.error('Approve review error:', error);
            Utils.showNotification('Ошибка одобрения отзыва', 'error');
        }
    }

    static async deleteAdminReview(reviewId) {
        if (!confirm('Удалить этот отзыв?')) return;

        try {
            const response = await ApiClient.delete(`/api/admin/reviews/${reviewId}`);

            if (response.success) {
                Utils.showNotification('Отзыв удален', 'success');
                await this.loadReviews();
            } else {
                Utils.showNotification(response.error || 'Ошибка удаления', 'error');
            }
        } catch (error) {
            console.error('Delete review error:', error);
            Utils.showNotification('Ошибка удаления отзыва', 'error');
        }
    }

    // === Управление пользователями ===

    static filterUsers() {
        this.usersPage = 1;
        this.loadUsers();
    }

    static async editUser(userId) {
        try {
            const response = await ApiClient.get(`/api/user/${userId}`);
            // Здесь должна быть логика открытия формы редактирования пользователя
            Utils.showNotification('Редактирование пользователя (в разработке)', 'info');
        } catch (error) {
            console.error('Edit user error:', error);
        }
    }

    static async toggleUserStatus(userId, isActive) {
        const action = isActive ? 'разблокировать' : 'заблокировать';

        if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} пользователя?`)) return;

        try {
            const response = await ApiClient.patch(`/api/admin/users/${userId}/status`, {
                isActive
            });

            if (response.success) {
                Utils.showNotification(`Пользователь ${isActive ? 'разблокирован' : 'заблокирован'}`, 'success');
                await this.loadUsers();
            } else {
                Utils.showNotification(response.error || 'Ошибка изменения статуса', 'error');
            }
        } catch (error) {
            console.error('Toggle user status error:', error);
            Utils.showNotification('Ошибка изменения статуса пользователя', 'error');
        }
    }

    static async deleteUser(userId) {
        if (!confirm('Удалить пользователя и все его отзывы?')) return;

        try {
            const response = await ApiClient.delete(`/api/admin/users/${userId}`);

            if (response.success) {
                Utils.showNotification('Пользователь удален', 'success');
                await this.loadUsers();
            } else {
                Utils.showNotification(response.error || 'Ошибка удаления', 'error');
            }
        } catch (error) {
            console.error('Delete user error:', error);
            Utils.showNotification('Ошибка удаления пользователя', 'error');
        }
    }

    // === Управление жанрами ===

    static showAddGenreModal() {
        document.getElementById('addGenreModal').style.display = 'flex';
    }

    static closeAddGenreModal() {
        document.getElementById('addGenreModal').style.display = 'none';
        document.getElementById('addGenreForm').reset();
    }

    static async addGenre(event) {
        event.preventDefault();

        const name = document.getElementById('genreName').value;

        try {
            const response = await ApiClient.post('/api/admin/genres', { name });

            if (response.success) {
                Utils.showNotification('Жанр добавлен', 'success');
                this.closeAddGenreModal();
                await this.loadGenres();
            } else {
                Utils.showNotification(response.error || 'Ошибка добавления', 'error');
            }
        } catch (error) {
            console.error('Add genre error:', error);
            Utils.showNotification('Ошибка добавления жанра', 'error');
        }
    }

    static editGenre(genreId, currentName) {
        const newName = prompt('Введите новое название жанра:', currentName);

        if (newName && newName !== currentName) {
            this.updateGenre(genreId, newName);
        }
    }

    static async updateGenre(genreId, name) {
        try {
            const response = await ApiClient.put(`/api/admin/genres/${genreId}`, { name });

            if (response.success) {
                Utils.showNotification('Жанр обновлен', 'success');
                await this.loadGenres();
            } else {
                Utils.showNotification(response.error || 'Ошибка обновления', 'error');
            }
        } catch (error) {
            console.error('Update genre error:', error);
            Utils.showNotification('Ошибка обновления жанра', 'error');
        }
    }

    static async deleteGenre(genreId) {
        if (!confirm('Удалить жанр? Это не затронет фильмы, только связь.')) return;

        try {
            const response = await ApiClient.delete(`/api/admin/genres/${genreId}`);

            if (response.success) {
                Utils.showNotification('Жанр удален', 'success');
                await this.loadGenres();
            } else {
                Utils.showNotification(response.error || 'Ошибка удаления', 'error');
            }
        } catch (error) {
            console.error('Delete genre error:', error);
            Utils.showNotification('Ошибка удаления жанра', 'error');
        }
    }

    // === Системные настройки ===

    static saveSystemSettings() {
        const settings = {
            siteName: document.getElementById('siteName').value,
            siteDescription: document.getElementById('siteDescription').value,
            moderateReviews: document.getElementById('moderateReviews').checked,
            autoApprove: document.getElementById('autoApprove').checked,
            requireEmailVerification: document.getElementById('requireEmailVerification').checked,
            allowRegistration: document.getElementById('allowRegistration').checked
        };

        // В реальном приложении здесь отправка на сервер
        localStorage.setItem('systemSettings', JSON.stringify(settings));
        Utils.showNotification('Настройки сохранены', 'success');
    }
}

// Пагинация
function prevMoviesPage() {
    if (AdminManager.moviesPage > 1) {
        AdminManager.moviesPage--;
        AdminManager.loadMovies();
    }
}

function nextMoviesPage() {
    if (AdminManager.moviesPage < AdminManager.totalMoviesPages) {
        AdminManager.moviesPage++;
        AdminManager.loadMovies();
    }
}

function prevUsersPage() {
    if (AdminManager.usersPage > 1) {
        AdminManager.usersPage--;
        AdminManager.loadUsers();
    }
}

function nextUsersPage() {
    if (AdminManager.usersPage < AdminManager.totalUsersPages) {
        AdminManager.usersPage++;
        AdminManager.loadUsers();
    }
}

// Глобальные функции
function switchAdminTab(tabId) {
    AdminManager.switchAdminTab(tabId);
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await AdminManager.init();
});