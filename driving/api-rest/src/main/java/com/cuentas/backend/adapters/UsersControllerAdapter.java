package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.User;
import com.cuentas.backend.domain.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UsersControllerAdapter.class);

    private final UserServicePort userService;

    public UsersControllerAdapter(UserServicePort userService) {
        this.userService = userService;
    }

    // ===============================
    // CRUD USUARIOS
    // ===============================

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        logger.info("Creando usuario: {}", user);
        User created = userService.createUser(user);
        logger.info("Respuesta createUser: {}", created);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        logger.info("Obteniendo usuario con userId={}", userId);
        User user = userService.getUserById(userId);
        if (user == null) {
            logger.info("Usuario no encontrado para userId={}", userId);
            return ResponseEntity.notFound().build();
        }
        logger.info("Respuesta getUserById: {}", user);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("Listando todos los usuarios");
        List<User> users = userService.getAllUsers();
        logger.info("Respuesta getAllUsers: {}", users);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User user) {
        logger.info("Actualizando usuario con userId={}, user={}", userId, user);
        User updated = userService.updateUser(userId, user);
        if (updated == null) {
            logger.info("Usuario no encontrado para actualizar userId={}", userId);
            return ResponseEntity.notFound().build();
        }
        logger.info("Respuesta updateUser: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        logger.info("Eliminando usuario con userId={}", userId);
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            logger.info("Usuario eliminado userId={}", userId);
            return ResponseEntity.noContent().build();
        } else {
            logger.info("Usuario no encontrado para eliminar userId={}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    // ===============================
    // CONFIGURACIÓN DEL USUARIO
    // ===============================

    @GetMapping("/{userId}/settings")
    public ResponseEntity<UserSettings> getUserSettings(@PathVariable Long userId) {
        logger.info("Obteniendo configuración para userId={}", userId);
        UserSettings settings = userService.getUserSettings(userId);
        if (settings == null) {
            logger.info("Configuración no encontrada para userId={}", userId);
            return ResponseEntity.notFound().build();
        }
        logger.info("Respuesta getUserSettings: {}", settings);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/{userId}/settings")
    public ResponseEntity<UserSettings> updateUserSettings(
            @PathVariable Long userId,
            @RequestBody UserSettings settings) {
        logger.info("Actualizando configuración para userId={}, settings={}", userId, settings);
        UserSettings updated = userService.updateUserSettings(userId, settings);
        if (updated == null) {
            logger.info("Configuración no encontrada para actualizar userId={}", userId);
            return ResponseEntity.notFound().build();
        }
        logger.info("Respuesta updateUserSettings: {}", updated);
        return ResponseEntity.ok(updated);
    }
}
