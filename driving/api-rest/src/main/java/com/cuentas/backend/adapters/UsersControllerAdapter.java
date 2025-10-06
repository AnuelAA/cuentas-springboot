package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.User;
import com.cuentas.backend.domain.UserSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersControllerAdapter {

    private final UserServicePort userService;

    public UsersControllerAdapter(UserServicePort userService) {
        this.userService = userService;
    }

    // ===============================
    // CRUD USUARIOS
    // ===============================

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User user) {
        User updated = userService.updateUser(userId, user);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        boolean deleted = userService.deleteUser(userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ===============================
    // CONFIGURACIÓN DEL USUARIO
    // ===============================

    @GetMapping("/{userId}/settings")
    public ResponseEntity<UserSettings> getUserSettings(@PathVariable Long userId) {
        UserSettings settings = userService.getUserSettings(userId);
        if (settings == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/{userId}/settings")
    public ResponseEntity<UserSettings> updateUserSettings(
            @PathVariable Long userId,
            @RequestBody UserSettings settings) {
        UserSettings updated = userService.updateUserSettings(userId, settings);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

}