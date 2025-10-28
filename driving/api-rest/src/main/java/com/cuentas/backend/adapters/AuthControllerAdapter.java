package com.cuentas.backend.adapters;

import com.cuentas.backend.application.services.AuthService;
import com.cuentas.backend.domain.AuthResponse;
import com.cuentas.backend.domain.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthControllerAdapter.class);

    private final AuthService authService;

    public AuthControllerAdapter(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Intento de login para email={}", loginRequest.getEmail());
        try {
            AuthResponse response = authService.login(loginRequest);
            logger.info("Login exitoso para userId={}", response.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error en login: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}

