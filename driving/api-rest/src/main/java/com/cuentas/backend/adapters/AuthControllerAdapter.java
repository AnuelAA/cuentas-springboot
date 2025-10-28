package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.UserServicePort;
import com.cuentas.backend.application.services.AuthService;
import com.cuentas.backend.domain.AuthResponse;
import com.cuentas.backend.domain.LoginRequest;
import com.cuentas.backend.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthControllerAdapter.class);

    private final AuthService authService;
    private final UserServicePort userService;

    public AuthControllerAdapter(AuthService authService, UserServicePort userService) {
        this.authService = authService;
        this.userService = userService;
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

    /**
     * Endpoint temporal para resetear contraseña de un usuario.
     * Body: {"email": "usuario@example.com", "newPassword": "nueva_contraseña"}
     * 
     * NOTA: Este endpoint debería protegerse o eliminarse en producción.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        
        if (email == null || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y nueva contraseña son requeridos"));
        }
        
        try {
            User user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            user.setPassword(newPassword);
            userService.updateUser(user.getUserId(), user);
            
            logger.info("Contraseña resetada para usuario email={}, userId={}", email, user.getUserId());
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente", "email", email));
        } catch (Exception e) {
            logger.error("Error al resetear contraseña: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error al actualizar contraseña"));
        }
    }
}

