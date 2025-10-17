package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ClaudeServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

@Service
public class ClaudeServiceUseCase implements ClaudeServicePort {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceUseCase.class);

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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

    // SQL (tomadas del patrón de ExcelNewServiceUseCase)
    private static final String SQL_ASSETS_WITH_VALUE_BY_DATE =
            "SELECT a.asset_id, a.name, at.name AS asset_type_name, a.acquisition_date, a.acquisition_value, av.current_value " +
                    "FROM assets a " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "INNER JOIN asset_values av ON av.asset_id = a.asset_id AND av.valuation_date = ? " +
                    "WHERE a.user_id = ? ORDER BY a.asset_id";

    private static final String SQL_ASSETS_WITH_LATEST =
            "SELECT a.asset_id, a.name, at.name AS asset_type_name, a.acquisition_date, a.acquisition_value, av.current_value " +
                    "FROM assets a " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "LEFT JOIN LATERAL (SELECT current_value FROM asset_values av WHERE av.asset_id = a.asset_id ORDER BY valuation_date DESC LIMIT 1) av ON true " +
                    "WHERE a.user_id = ? ORDER BY a.asset_id";

    private static final String SQL_LIABS_WITH_VALUE_BY_DATE =
            "SELECT l.liability_id, l.name, lt.name AS liability_type_name, l.principal_amount, i.type AS interest_type, i.annual_rate, l.start_date, lv.end_date, lv.outstanding_balance " +
                    "FROM liabilities l " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "INNER JOIN liability_values lv ON lv.liability_id = l.liability_id AND lv.valuation_date = ? " +
                    "LEFT JOIN interests i ON i.liability_id = l.liability_id AND i.start_date = l.start_date " +
                    "WHERE l.user_id = ? ORDER BY l.liability_id";

    private static final String SQL_LIABS_WITH_LATEST =
            "SELECT l.liability_id, l.name, lt.name AS liability_type_name, l.principal_amount, i.type AS interest_type, i.annual_rate, l.start_date, lv.end_date, lv.outstanding_balance " +
                    "FROM liabilities l " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "LEFT JOIN interests i ON i.liability_id = l.liability_id AND i.start_date = l.start_date " +
                    "LEFT JOIN LATERAL (SELECT end_date, outstanding_balance FROM liability_values lv WHERE lv.liability_id = l.liability_id ORDER BY valuation_date DESC LIMIT 1) lv ON true " +
                    "WHERE l.user_id = ? ORDER BY l.liability_id";

    private static final String SQL_TRANSACTIONS_BY_DATE =
            "SELECT t.transaction_id, t.transaction_type, t.amount, t.transaction_date, t.category_id, c.name AS category_name, " +
                    "t.asset_id, a.name AS asset_name, t.liability_id, l.name AS liability_name, t.related_asset_id, ra.name AS related_asset_name, t.description " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category_id = c.category_id " +
                    "LEFT JOIN assets a ON t.asset_id = a.asset_id " +
                    "LEFT JOIN liabilities l ON t.liability_id = l.liability_id " +
                    "LEFT JOIN assets ra ON t.related_asset_id = ra.asset_id " +
                    "WHERE t.user_id = ? AND t.transaction_date = ? " +
                    "ORDER BY t.transaction_date DESC, t.transaction_id";

    private static final String SQL_TRANSACTIONS_ALL =
            "SELECT t.transaction_id, t.transaction_type, t.amount, t.transaction_date, t.category_id, c.name AS category_name, " +
                    "t.asset_id, a.name AS asset_name, t.liability_id, l.name AS liability_name, t.related_asset_id, ra.name AS related_asset_name, t.description " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category_id = c.category_id " +
                    "LEFT JOIN assets a ON t.asset_id = a.asset_id " +
                    "LEFT JOIN liabilities l ON t.liability_id = l.liability_id " +
                    "LEFT JOIN assets ra ON t.related_asset_id = ra.asset_id " +
                    "WHERE t.user_id = ? " +
                    "ORDER BY t.transaction_date DESC, t.transaction_id";

    public ClaudeServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getResponse(Long userId, String message) {
        if (userId == null) throw new IllegalArgumentException("userId obligatorio");
        if (message == null) message = "";

        log.info("Construyendo contexto para userId={} (petición chat)", userId);

        LocalDate requestedDate = parseDateFromMessage(message);
        String context = buildContext(userId, requestedDate);
        String prompt = buildPrompt(context, message);

        log.debug("Prompt length: {} chars", prompt.length());

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

    // Construye contexto: si date != null usa queries por fecha; si es null usa últimas valoraciones / todas las txs
    private String buildContext(Long userId, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

        sb.append("Contexto usuario id=").append(userId).append("\n\n");

        // CATEGORÍAS
        sb.append("CATEGORÍAS:\n");
        try {
            String sqlCategories = "SELECT category_id, parent_category_id, name, description, created_at FROM categories WHERE user_id = ?";
            List<Map<String, Object>> cats = jdbcTemplate.queryForList(sqlCategories, userId);
            if (cats.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : cats) {
                    sb.append(String.format("- category_id=%s | parent_category_id=%s | name=%s | description=%s | created_at=%s\n",
                            safeToString(r.get("category_id")),
                            safeToString(r.get("parent_category_id")),
                            safeToString(r.get("name")),
                            safeToString(r.get("description")),
                            r.get("created_at") != null ? r.get("created_at").toString() : "NULL"
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando categorías: {}", ex.getMessage());
            sb.append("- error consultando categorías\n");
        }
        sb.append("\n");

        // TIPOS DE ASSET
        sb.append("TIPOS_DE_ASSET:\n");
        try {
            String sqlAssetTypes = "SELECT asset_type_id, name, description FROM asset_types ORDER BY asset_type_id";
            List<Map<String, Object>> ats = jdbcTemplate.queryForList(sqlAssetTypes);
            if (ats.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : ats) {
                    sb.append(String.format("- asset_type_id=%s | name=%s | description=%s\n",
                            safeToString(r.get("asset_type_id")),
                            safeToString(r.get("name")),
                            safeToString(r.get("description"))
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando tipos de asset: {}", ex.getMessage());
            sb.append("- error consultando tipos de asset\n");
        }
        sb.append("\n");

        // TIPOS DE LIABILITY
        sb.append("TIPOS_DE_LIABILITY:\n");
        try {
            String sqlLiabilityTypes = "SELECT liability_type_id, name, description FROM liability_types ORDER BY liability_type_id";
            List<Map<String, Object>> lts = jdbcTemplate.queryForList(sqlLiabilityTypes);
            if (lts.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : lts) {
                    sb.append(String.format("- liability_type_id=%s | name=%s | description=%s\n",
                            safeToString(r.get("liability_type_id")),
                            safeToString(r.get("name")),
                            safeToString(r.get("description"))
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando tipos de liability: {}", ex.getMessage());
            sb.append("- error consultando tipos de liability\n");
        }
        sb.append("\n");

        // INTERESES (solo los asociados a liabilities del usuario)
        sb.append("INTERESTS:\n");
        try {
            String sqlInterests = "SELECT i.interest_id, i.liability_id, i.type, i.annual_rate, i.start_date, i.created_at " +
                    "FROM interests i JOIN liabilities l ON i.liability_id = l.liability_id WHERE l.user_id = ? ORDER BY i.interest_id";
            List<Map<String, Object>> ints = jdbcTemplate.queryForList(sqlInterests, userId);
            if (ints.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : ints) {
                    sb.append(String.format("- interest_id=%s | liability_id=%s | type=%s | annual_rate=%s | start_date=%s | created_at=%s\n",
                            safeToString(r.get("interest_id")),
                            safeToString(r.get("liability_id")),
                            safeToString(r.get("type")),
                            safeNumber(r.get("annual_rate")),
                            dateOrFormat(r.get("start_date"), dtf),
                            r.get("created_at") != null ? r.get("created_at").toString() : "NULL"
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando interests: {}", ex.getMessage());
            sb.append("- error consultando interests\n");
        }
        sb.append("\n");

        // ACTIVOS
        sb.append("ACTIVOS:\n");
        try {
            List<Map<String, Object>> assets;
            if (date != null) {
                assets = jdbcTemplate.queryForList(SQL_ASSETS_WITH_VALUE_BY_DATE, date, userId);
                sb.append(String.format("(valor en %s)\n", date.format(dtf)));
            } else {
                assets = jdbcTemplate.queryForList(SQL_ASSETS_WITH_LATEST, userId);
                sb.append("(última valoración disponible por activo)\n");
            }
            if (assets.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : assets) {
                    sb.append(String.format("- %s | tipo=%s | adquisición=%s | acquisition_value=%s | current_value=%s\n",
                            safeToString(r.get("name")),
                            safeToString(r.get("asset_type_name")),
                            dateOrFormat(r.get("acquisition_date"), dtf),
                            safeNumber(r.get("acquisition_value")),
                            safeNumber(r.get("current_value"))
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando activos: {}", ex.getMessage());
            sb.append("- error consultando activos\n");
        }
        sb.append("\n");

        // PASIVOS
        sb.append("PASIVOS:\n");
        try {
            List<Map<String, Object>> liabs;
            if (date != null) {
                liabs = jdbcTemplate.queryForList(SQL_LIABS_WITH_VALUE_BY_DATE, date, userId);
                sb.append(String.format("(valor en %s)\n", date.format(dtf)));
            } else {
                liabs = jdbcTemplate.queryForList(SQL_LIABS_WITH_LATEST, userId);
                sb.append("(última valoración disponible por pasivo)\n");
            }
            if (liabs.isEmpty()) {
                sb.append("- ninguno\n");
            } else {
                for (Map<String, Object> r : liabs) {
                    sb.append(String.format("- %s | tipo=%s | principal=%s | rate=%s | start=%s | end=%s | outstanding=%s\n",
                            safeToString(r.get("name")),
                            safeToString(r.get("liability_type_name")),
                            safeNumber(r.get("principal_amount")),
                            safeNumber(r.get("annual_rate")),
                            dateOrFormat(r.get("start_date"), dtf),
                            dateOrFormat(r.get("end_date"), dtf),
                            safeNumber(r.get("outstanding_balance"))
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando pasivos: {}", ex.getMessage());
            sb.append("- error consultando pasivos\n");
        }
        sb.append("\n");

        // TRANSACCIONES
        sb.append("TRANSACCIONES:\n");
        try {
            List<Map<String, Object>> txs;
            if (date != null) {
                txs = jdbcTemplate.queryForList(SQL_TRANSACTIONS_BY_DATE, userId, date);
                sb.append(String.format("(transacciones en %s)\n", date.format(dtf)));
            } else {
                txs = jdbcTemplate.queryForList(SQL_TRANSACTIONS_ALL, userId);
                sb.append("(todas las transacciones, más recientes primero)\n");
            }
            if (txs.isEmpty()) {
                sb.append("- ninguna\n");
            } else {
                for (Map<String, Object> r : txs) {
                    sb.append(String.format("- id=%s | %s | %s€ | date=%s | cat=%s | asset=%s | liability=%s | related_asset=%s | %s\n",
                            safeToString(r.get("transaction_id")),
                            safeToString(r.get("transaction_type")),
                            safeNumber(r.get("amount")),
                            dateOrFormat(r.get("transaction_date"), dtf),
                            safeToString(r.get("category_name")),
                            safeToString(r.get("asset_name")),
                            safeToString(r.get("liability_name")),
                            safeToString(r.get("related_asset_name")),
                            safeToString(r.get("description"))
                    ));
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando transacciones: {}", ex.getMessage());
            sb.append("- error consultando transacciones\n");
        }
        sb.append("\n");

        // Totales rápidos
        try {
            String sqlTotals = "SELECT " +
                    "COALESCE((SELECT SUM(current_value) FROM asset_values av JOIN assets a ON av.asset_id = a.asset_id WHERE a.user_id = ?),0) AS total_assets, " +
                    "COALESCE((SELECT SUM(outstanding_balance) FROM liability_values lv JOIN liabilities l ON lv.liability_id = l.liability_id WHERE l.user_id = ?),0) AS total_liabilities";
            Map<String, Object> totals = jdbcTemplate.queryForMap(sqlTotals, userId, userId);
            sb.append("RESUMEN:\n");
            sb.append(String.format("- total_assets=%s | total_liabilities=%s\n",
                    safeNumber(totals.get("total_assets")), safeNumber(totals.get("total_liabilities"))));
        } catch (DataAccessException ex) {
            sb.append("- no se pudieron calcular totales\n");
        }

        return sb.toString();
    }

    private String buildPrompt(String context, String userMessage) {
        return String.join("\n",
                "Eres un asistente financiero experto. Usa SOLO el contexto proporcionado para responder preguntas específicas sobre las finanzas del usuario.",
                "Si la pregunta no se puede responder con los datos proporcionados, dilo claramente y sugiere qué dato falta.",
                "",
                "=== CONTEXTO ===",
                context,
                "=== PREGUNTA ===",
                userMessage == null ? "" : userMessage,
                "",
                "RESPONDE DE FORMA CLARA, NUMÉRICA CUANDO SEA POSIBLE, Y RESUMIDA (máx. 300 palabras)."
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", claudeApiKey);
        // Versión que coincide con el ejemplo funcional; ajustar si es necesario
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("Invocando Claude API: url={} model={} temperature={} max_tokens={} promptLen={}",
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
        log.debug("Claude raw response (trunc): {}", respBody != null ? (respBody.length() > 1000 ? respBody.substring(0, 1000) + "..." : respBody) : "null");
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

    // utilitarios
    private static String safeToString(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String safeNumber(Object o) {
        if (o == null) return "0";
        if (o instanceof Number) {
            if (o instanceof BigDecimal) return ((BigDecimal) o).toPlainString();
            return String.valueOf(((Number) o).doubleValue());
        }
        return o.toString();
    }

    private static String firstN(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }

    private static String dateOrFormat(Object dateObj, DateTimeFormatter dtf) {
        if (dateObj == null) return "";
        try {
            if (dateObj instanceof java.sql.Date) {
                return ((java.sql.Date) dateObj).toLocalDate().format(dtf);
            } else if (dateObj instanceof java.time.LocalDate) {
                return ((java.time.LocalDate) dateObj).format(dtf);
            } else {
                return LocalDate.parse(dateObj.toString()).format(dtf);
            }
        } catch (Exception e) {
            return dateObj.toString();
        }
    }
}