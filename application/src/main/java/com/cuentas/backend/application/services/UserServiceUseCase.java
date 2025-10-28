package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.User;
import com.cuentas.backend.domain.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceUseCase implements UserServicePort {

    private static final Logger log = LoggerFactory.getLogger(UserServiceUseCase.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserServiceUseCase(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String SQL_INSERT_USER =
            "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, NOW()) RETURNING user_id";
    private static final String SQL_SELECT_USER =
            "SELECT * FROM users WHERE user_id = ?";
    private static final String SQL_SELECT_USER_BY_EMAIL =
            "SELECT * FROM users WHERE email = ?";
    private static final String SQL_SELECT_ALL_USERS =
            "SELECT * FROM users";
    private static final String SQL_UPDATE_USER =
            "UPDATE users SET name = ?, email = ?, password_hash = ?, updated_at = NOW() WHERE user_id = ?";
    private static final String SQL_DELETE_USER =
            "DELETE FROM users WHERE user_id = ?";

    private static final String SQL_SELECT_SETTINGS =
            "SELECT * FROM user_settings WHERE user_id = ?";
    private static final String SQL_UPDATE_SETTINGS =
            "UPDATE user_settings SET dark_mode = ?, language = ?, notifications_email = ?, updated_at = NOW() WHERE user_id = ?";

    // =======================
    // CRUD Users
    // =======================
    @Override
    @Transactional
    public User createUser(User user) {
        log.info("Creando usuario: {}", user.getEmail());
        // Verificar que la contraseña no esté vacía
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        // Encriptar la contraseña antes de guardarla
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        Long id = jdbcTemplate.queryForObject(SQL_INSERT_USER, Long.class,
                user.getName(), user.getEmail(), encodedPassword);
        user.setUserId(id);
        user.setCreatedAt(LocalDateTime.now());
        // No devolver la contraseña encriptada en el objeto User devuelto
        user.setPassword(null);
        log.info("Usuario creado con id={}", id);
        return user;
    }

    @Override
    public User getUserById(Long userId) {
        try {
            User user = jdbcTemplate.queryForObject(SQL_SELECT_USER, new UserRowMapper(), userId);
            if (user != null) {
                user.setPassword(null); // No exponer contraseña
            }
            return user;
        } catch (DataAccessException e) {
            log.warn("Usuario no encontrado con id={}", userId);
            return null;
        }
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            // Este método se usa para autenticación, mantener la contraseña para verificación
            return jdbcTemplate.queryForObject(SQL_SELECT_USER_BY_EMAIL, new UserRowMapper(), email);
        } catch (DataAccessException e) {
            log.warn("Usuario no encontrado con email={}", email);
            return null;
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = jdbcTemplate.query(SQL_SELECT_ALL_USERS, new UserRowMapper());
        // No exponer contraseñas en la lista
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    @Transactional
    public User updateUser(Long userId, User user) {
        log.info("Actualizando usuario id={}", userId);
        String passwordToUpdate = user.getPassword();
        if (passwordToUpdate != null && !passwordToUpdate.isEmpty()) {
            // Verificar si ya está cifrada con BCrypt antes de cifrar de nuevo
            if (!passwordToUpdate.startsWith("$2a$") && !passwordToUpdate.startsWith("$2b$")) {
                // No está cifrada, cifrarla
                passwordToUpdate = passwordEncoder.encode(passwordToUpdate);
            }
            // Si ya está cifrada, usar directamente (para migraciones internas)
        } else {
            // Si no se proporciona contraseña, mantener la actual (consultar directamente de BD)
            try {
                User existingUser = jdbcTemplate.queryForObject(SQL_SELECT_USER, new UserRowMapper(), userId);
                if (existingUser != null) {
                    passwordToUpdate = existingUser.getPassword();
                }
            } catch (DataAccessException e) {
                log.warn("Usuario no encontrado para actualizar id={}", userId);
                return null;
            }
        }
        jdbcTemplate.update(SQL_UPDATE_USER,
                user.getName(), user.getEmail(), passwordToUpdate, userId);
        return getUserById(userId);
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        log.info("Borrando usuario id={}", userId);
        int rows = jdbcTemplate.update(SQL_DELETE_USER, userId);
        boolean deleted = rows > 0;
        log.info("Usuario id={} borrado: {}", userId, deleted);
        return deleted;
    }

    // =======================
    // User Settings
    // =======================
    @Override
    public UserSettings getUserSettings(Long userId) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_SETTINGS, new UserSettingsRowMapper(), userId);
        } catch (DataAccessException e) {
            log.warn("No se encontraron settings para usuario id={}", userId);
            return null;
        }
    }

    @Override
    @Transactional
    public UserSettings updateUserSettings(Long userId, UserSettings settings) {
        log.info("Actualizando settings para usuario id={}", userId);
        jdbcTemplate.update(SQL_UPDATE_SETTINGS,
                settings.getDarkMode(),
                settings.getLanguage(),
                settings.getNotificationsEmail(),
                userId);
        return getUserSettings(userId);
    }

    // =======================
    // RowMappers
    // =======================
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User u = new User();
            u.setUserId(rs.getLong("user_id"));
            u.setName(rs.getString("name"));
            u.setEmail(rs.getString("email"));
            // Mantener el password_hash para uso interno (autenticación)
            // Se puede limpiar en métodos públicos si es necesario
            u.setPassword(rs.getString("password_hash"));
            u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            var updated = rs.getTimestamp("updated_at");
            if (updated != null) u.setUpdatedAt(updated.toLocalDateTime());
            return u;
        }
    }

    private static class UserSettingsRowMapper implements RowMapper<UserSettings> {
        @Override
        public UserSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserSettings s = new UserSettings();
            s.setSettingId(rs.getLong("setting_id"));
            s.setUserId(rs.getLong("user_id"));
            s.setDarkMode(rs.getBoolean("dark_mode"));
            s.setLanguage(rs.getString("language"));
            s.setNotificationsEmail(rs.getBoolean("notifications_email"));
            s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            var updated = rs.getTimestamp("updated_at");
            if (updated != null) s.setUpdatedAt(updated.toLocalDateTime());
            return s;
        }
    }
}