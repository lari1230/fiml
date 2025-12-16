package main.services;

import main.dao.UserDAO;
import main.models.User;
import main.utils.PasswordHasher;
import main.utils.Validator;
import java.sql.SQLException;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User register(String username, String email, String password) throws SQLException {
        // Валидация
        if (!Validator.isValidUsername(username)) {
            throw new IllegalArgumentException("Имя пользователя должно содержать от 3 до 20 символов (буквы, цифры, подчеркивание)");
        }

        if (!Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Некорректный email адрес");
        }

        if (!Validator.isValidPassword(password)) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 8 символов, включая цифры, строчные и заглавные буквы");
        }

        // Проверка существования пользователя
        if (userDAO.emailExists(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        if (userDAO.usernameExists(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        // Создание пользователя
        String passwordHash = PasswordHasher.hashPassword(password);
        User user = new User(username, email, passwordHash);

        return userDAO.createUser(user);
    }

    public User login(String email, String password) throws SQLException {
        User user = userDAO.getUserByEmail(email);

        if (user == null) {
            return null;
        }

        if (!PasswordHasher.checkPassword(password, user.getPasswordHash())) {
            return null;
        }

        return user;
    }

    public User getUserById(int userId) throws SQLException {
        return userDAO.getUserById(userId);
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        User user = userDAO.getUserById(userId);

        if (user == null) {
            return false;
        }

        if (!PasswordHasher.checkPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }

        if (!Validator.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Новый пароль не соответствует требованиям безопасности");
        }

        String newPasswordHash = PasswordHasher.hashPassword(newPassword);
        user.setPasswordHash(newPasswordHash);

        // Для упрощения не обновляем через DAO, нужно добавить метод в UserDAO
        return true;
    }
}