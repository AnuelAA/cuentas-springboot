package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.domain.AuthResponse;
import com.cuentas.backend.domain.LoginRequest;
import com.cuentas.backend.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserServicePort userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserServicePort userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Intento de login para email={}", loginRequest.getEmail());
        
        User user = userService.getUserByEmail(loginRequest.getEmail());
        if (user == null) {
            log.warn("Usuario no encontrado con email={}", loginRequest.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        String storedPassword = user.getPassword();
        boolean passwordMatches = false;

        if (storedPassword == null) {
            log.warn("Contraseña nula para email={}", loginRequest.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        // Verificar si la contraseña está encriptada con BCrypt
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            // Contraseña en BCrypt, usar matches normal
            log.debug("Comparando contraseña BCrypt para email={}", loginRequest.getEmail());
            passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), storedPassword);
            log.debug("Resultado comparación BCrypt: {}", passwordMatches);
        } else {
            // Contraseña antigua en texto plano, comparar directamente
            log.info("Contraseña antigua detectada para email={} (formato: {}), migrando a BCrypt", 
                    loginRequest.getEmail(), storedPassword.length() > 20 ? "hash posible" : "texto plano");
            passwordMatches = storedPassword.equals(loginRequest.getPassword());
            log.debug("Resultado comparación texto plano: {}", passwordMatches);
            
            // Si coincide, migrar a BCrypt
            if (passwordMatches) {
                String encodedPassword = passwordEncoder.encode(loginRequest.getPassword());
                user.setPassword(encodedPassword);
                userService.updateUser(user.getUserId(), user);
                log.info("Contraseña migrada a BCrypt para userId={}", user.getUserId());
            }
        }

        if (!passwordMatches) {
            log.warn("Contraseña incorrecta para email={}", loginRequest.getEmail());
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

