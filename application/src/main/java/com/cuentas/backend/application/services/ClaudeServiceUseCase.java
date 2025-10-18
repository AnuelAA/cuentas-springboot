package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ClaudeServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

@Service
public class ClaudeServiceUseCase implements ClaudeServicePort {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceUseCase.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FinancialContextBuilder contextBuilder;

    @Value("${claude.api.key:}")
    private String claudeApiKey;

    @Value("${claude.model:claude-3-haiku-20240307}")
    private String claudeModel;

    @Value("${claude.temperature:0.4}")
    private double claudeTemperature;

    @Value("${claude.maxTokens:1500}")
    private int claudeMaxTokens;
    // Anthropic endpoint (ajustar si usas proxy)
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";


    public ClaudeServiceUseCase(FinancialContextBuilder contextBuilder) {
        // RestTemplate con buffering para poder leer response body desde el interceptor
        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        this.restTemplate.getInterceptors().add(new RequestSizeLoggingInterceptor());
        this.objectMapper = new ObjectMapper();
        this.contextBuilder = contextBuilder;
    }

    @Override
    public String getResponse(Long userId, String message) {
        if (userId == null) throw new IllegalArgumentException("userId obligatorio");
        if (message == null) message = "";

        log.info("Construyendo contexto para userId={} (petición chat)", userId);

        LocalDate requestedDate = parseDateFromMessage(message);
        String context = contextBuilder.buildContextWithQuery(userId, requestedDate, message);
        String prompt = buildPrompt(context, message);

        log.info("Prompt length: {} chars", prompt.length());

        log.info("prompt construido: {}", prompt);
        if (claudeApiKey == null || claudeApiKey.isBlank()) {
            log.warn("claudeApiKey no configurada, devolviendo prompt para debug");
            return prompt;
        }

        try {
            String claudeAnswer = callClaudeApi(prompt);
            log.info("Respuesta Claude (len={}): {}", claudeAnswer == null ? 0 : claudeAnswer.length(), firstN(claudeAnswer, 200));
            return claudeAnswer;
        } catch (Exception ex) {
            log.error("Error llamando a Claude: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error llamando a servicio de IA", ex);
        }
    }


    private String buildPrompt(String context, String userMessage) {
        return String.join("\n",
                "Eres un asistente financiero personal. Analiza la información financiera del usuario y responde de manera clara y útil.",
                "",
                "**INSTRUCCIONES:**",
                "- Responde basándote ÚNICAMENTE en los datos proporcionados",
                "- Usa números y porcentajes cuando sea relevante",
                "- Sé conciso pero completo (máximo 200 palabras)",
                "- Si falta información, indícalo claramente",
                "- Usa un tono profesional pero cercano",
                "",
                "**DATOS FINANCIEROS:**",
                context,
                "",
                "**PREGUNTA DEL USUARIO:**",
                userMessage == null ? "" : userMessage,
                "",
                "**RESPUESTA:**"
        );
    }

    // Llamada HTTP a Claude (Anthropic)

    private String callClaudeApi(String prompt) throws Exception {
        if (claudeApiKey == null || claudeApiKey.isBlank()) {
            String msg = "claude.api.key no configurada";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // Construir body acorde al ejemplo funcional
        Map<String, Object> body = new HashMap<>();
        body.put("model", claudeModel);
        body.put("max_tokens", claudeMaxTokens);
        body.put("temperature", claudeTemperature);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);

        // Serializar a JSON y medir tamaño exacto en bytes (UTF-8)
        byte[] jsonBytes = objectMapper.writeValueAsBytes(body);
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        int bytesLen = jsonBytes.length;
        int charsLen = json.length();
        log.info("Claude request JSON size: {} bytes ({} chars)", bytesLen, charsLen);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", claudeApiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentLength(bytesLen); // opcional, explícito

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Invocando Claude API: url={} model={} temperature={} max_tokens={} promptLen={}",
                CLAUDE_API_URL, claudeModel, claudeTemperature, claudeMaxTokens, prompt == null ? 0 : prompt.length());

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.exchange(CLAUDE_API_URL, HttpMethod.POST, request, String.class);
        } catch (Exception ex) {
            String err = "Error calling Claude API: " + ex.getMessage();
            log.error(err, ex);
            throw new RuntimeException(err, ex);
        }

        if (!resp.getStatusCode().is2xxSuccessful()) {
            String err = String.format("Claude API responded with status %s and body: %s", resp.getStatusCode(), resp.getBody());
            log.error(err);
            throw new RuntimeException(err);
        }

        String respBody = resp.getBody();
        log.info("Claude raw response (trunc): {}", respBody != null ? (respBody.length() > 1000 ? respBody.substring(0, 1000) + "..." : respBody) : "null");
        if (respBody == null || respBody.isBlank()) return "";

        // Parsear la estructura conocida: "content": [ { "type":"text", "text": "..." }, ... ]
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(respBody);

            // 1) Contenido directo en "content" como array de piezas con "text"
            if (root.has("content") && root.path("content").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (com.fasterxml.jackson.databind.JsonNode part : root.path("content")) {
                    if (part.has("text")) {
                        sb.append(part.path("text").asText());
                    } else if (part.has("prompt")) {
                        sb.append(part.path("prompt").asText());
                    } else {
                        // si el nodo es textual directamente
                        if (part.isTextual()) sb.append(part.asText());
                    }
                    if (sb.length() > 0) sb.append("\n");
                }
                String out = sb.toString().trim();
                if (!out.isBlank()) return out;
            }

            // 2) Formato alternativo: content puede ser texto simple
            if (root.has("content") && root.path("content").isTextual()) {
                String t = root.path("content").asText();
                if (!t.isBlank()) return t.trim();
            }

            // 3) Campos tipo "message", "completion", "response", "output" (fallbacks)
            String[] candidates = new String[] {"message", "completion", "response", "output"};
            for (String c : candidates) {
                if (root.has(c)) {
                    com.fasterxml.jackson.databind.JsonNode node = root.path(c);
                    if (node.isTextual()) {
                        String t = node.asText();
                        if (!t.isBlank()) return t.trim();
                    } else if (node.isObject()) {
                        // buscar propiedades comunes
                        if (node.has("content") && node.path("content").isTextual()) return node.path("content").asText().trim();
                        if (node.has("text") && node.path("text").isTextual()) return node.path("text").asText().trim();
                        if (node.has("message") && node.path("message").has("content")) {
                            com.fasterxml.jackson.databind.JsonNode cont = node.path("message").path("content");
                            if (cont.isTextual()) return cont.asText().trim();
                        }
                    } else if (node.isArray() && node.size() > 0 && node.get(0).has("text")) {
                        return node.get(0).path("text").asText().trim();
                    }
                }
            }

            // 4) OpenAI-like: choices[0].message.content o choices[0].text
            if (root.has("choices") && root.path("choices").isArray() && root.path("choices").size() > 0) {
                com.fasterxml.jackson.databind.JsonNode choice = root.path("choices").get(0);
                if (choice.has("message") && choice.path("message").has("content")) {
                    com.fasterxml.jackson.databind.JsonNode cont = choice.path("message").path("content");
                    if (cont.isTextual()) return cont.asText().trim();
                }
                if (choice.has("text") && choice.path("text").isTextual()) {
                    return choice.path("text").asText().trim();
                }
            }

            // 5) Si no se pudo extraer, devolver to do el body como fallback
            return respBody.trim();
        } catch (Exception e) {
            log.warn("No se pudo parsear JSON de respuesta o extraer texto: {}. Devuelvo raw body.", e.getMessage());
            return respBody.trim();
        }
    }

    // Extrae fecha del mensaje: yyyy-MM-dd, dd/MM/yyyy, dd-MMM-yyyy (en inglés)
    private LocalDate parseDateFromMessage(String message) {
        if (message == null || message.isBlank()) return null;

        Pattern pIso = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher mIso = pIso.matcher(message);
        if (mIso.find()) {
            try {
                return LocalDate.parse(mIso.group(1), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {}
        }

        Pattern pSlash = Pattern.compile("(\\b\\d{2}/\\d{2}/\\d{4}\\b)");
        Matcher mSlash = pSlash.matcher(message);
        if (mSlash.find()) {
            try {
                return LocalDate.parse(mSlash.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}
        }

        Pattern pAdm = Pattern.compile("(\\b\\d{2}-[A-Za-z]{3}-\\d{4}\\b)");
        Matcher mAdm = pAdm.matcher(message);
        if (mAdm.find()) {
            try {
                return LocalDate.parse(mAdm.group(1), DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
            } catch (Exception ignored) {}
        }

        return null;
    }

    // Utility method
    private static String firstN(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}