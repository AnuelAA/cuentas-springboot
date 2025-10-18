package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.User;
import com.cuentas.backend.domain.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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

    private static final String SQL_INSERT_USER =
            "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, NOW()) RETURNING user_id";
    private static final String SQL_SELECT_USER =
            "SELECT * FROM users WHERE user_id = ?";
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

    public UserServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =======================
    // CRUD Users
    // =======================
    @Override
    @Transactional
    public User createUser(User user) {
        log.info("Creando usuario: {}", user.getEmail());
        Long id = jdbcTemplate.queryForObject(SQL_INSERT_USER, Long.class,
                user.getName(), user.getEmail(), user.getPassword());
        user.setUserId(id);
        user.setCreatedAt(LocalDateTime.now());
        log.info("Usuario creado con id={}", id);
        return user;
    }

    @Override
    public User getUserById(Long userId) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_USER, new UserRowMapper(), userId);
        } catch (DataAccessException e) {
            log.warn("Usuario no encontrado con id={}", userId);
            return null;
        }
    }

    @Override
    public List<User> getAllUsers() {
        return jdbcTemplate.query(SQL_SELECT_ALL_USERS, new UserRowMapper());
    }

    @Override
    @Transactional
    public User updateUser(Long userId, User user) {
        log.info("Actualizando usuario id={}", userId);
        jdbcTemplate.update(SQL_UPDATE_USER,
                user.getName(), user.getEmail(), user.getPassword(), userId);
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