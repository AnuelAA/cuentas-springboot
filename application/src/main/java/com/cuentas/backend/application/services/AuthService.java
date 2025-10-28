package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.AuthResponse;
import com.cuentas.backend.domain.LoginRequest;
import com.cuentas.backend.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserServicePort userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserServicePort userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Intento de login para email={}", loginRequest.getEmail());
        
        User user = userService.getUserByEmail(loginRequest.getEmail());
        if (user == null) {
            log.warn("Usuario no encontrado con email={}", loginRequest.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            log.warn("Contraseña nula para email={}", loginRequest.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        String inputPassword = loginRequest.getPassword();
        
        // Log para diagnóstico (sin exponer las contraseñas completas)
        log.info("Comparando contraseña - Input length: {}, Stored hash: {}", 
                inputPassword != null ? inputPassword.length() : 0,
                storedPassword != null && storedPassword.length() > 30 ? storedPassword.substring(0, 30) + "..." : storedPassword);
        log.info("Tipo PasswordEncoder: {}, Hash stored starts with: {}", 
                passwordEncoder.getClass().getSimpleName(),
                storedPassword != null && storedPassword.length() > 4 ? storedPassword.substring(0, 4) : "null");
        
        // Verificar contraseña (siempre en BCrypt)
        boolean passwordMatches = passwordEncoder.matches(inputPassword, storedPassword);
        
        log.info("Resultado comparación para email={}: {}", loginRequest.getEmail(), passwordMatches);
        
        // Si falla, intentar verificar si es un problema con el encoder
        if (!passwordMatches) {
            log.warn("Intento de re-cifrado para diagnóstico - Input password length: {}", inputPassword != null ? inputPassword.length() : 0);
            String testHash = passwordEncoder.encode(inputPassword);
            log.warn("Hash generado con mismo encoder empieza con: {}", testHash.length() > 30 ? testHash.substring(0, 30) + "..." : testHash);
        }
        
        if (!passwordMatches) {
            log.warn("Contraseña incorrecta para email={} - Hash almacenado empieza con: {}", 
                    loginRequest.getEmail(), 
                    storedPassword.length() > 20 ? storedPassword.substring(0, 20) + "..." : storedPassword);
            throw new RuntimeException("Credenciales inválidas");
        }

        // Generar token JWT
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getName());
        
        log.info("Login exitoso para userId={}, email={}", user.getUserId(), user.getEmail());
        
        return new AuthResponse(token, user.getUserId(), user.getEmail(), user.getName());
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}

