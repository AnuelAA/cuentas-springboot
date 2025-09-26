package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.*;

import java.util.List;

public interface UserServicePort {
    // CRUD Usuarios
    User createUser(User user);
    User getUserById(Long userId);
    List<User> getAllUsers();
    User updateUser(Long userId, User user);
    boolean deleteUser(Long userId);

    // Configuración
    UserSettings getUserSettings(Long userId);
    UserSettings updateUserSettings(Long userId, UserSettings settings);
}
