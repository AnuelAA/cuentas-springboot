package com.cuentas.backend.adapters;


import com.cuentas.backend.application.ports.driving.ClaudeServicePort;
import com.cuentas.backend.domain.ChatRequest;
import com.cuentas.backend.domain.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/chat")
public class ChatControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ChatControllerAdapter.class);

    private final ClaudeServicePort claudeService;

    public ChatControllerAdapter(ClaudeServicePort claudeService) {
        this.claudeService = claudeService;
    }

    /**
     * Recibe un mensaje del frontend, construye contexto (interno) y delega en ClaudeServicePort.
     * El body es { "message": "..." }.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@PathVariable Long userId, @RequestBody ChatRequest request) {
        logger.info("Chat request para userId={} - mensaje='{}'", userId, request == null ? null : request.getMessage());

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Mensaje vacío"));
        }

        try {
            String reply = claudeService.getResponse(userId, request.getMessage());
            ChatResponse response = new ChatResponse(reply);
            logger.info("Chat response para userId={} -> {} chars", userId, response.getResponse() == null ? 0 : response.getResponse().length());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error en chat para userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(new ChatResponse("Error interno procesando la petición"));
        }
    }
}