package com.cuentas.backend.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FinancialContextBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(FinancialContextBuilder.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    
    private final JdbcTemplate jdbcTemplate;
    
    // SQL Queries optimizadas
    private static final String SQL_ASSETS_WITH_VALUE_BY_DATE =
            "SELECT a.name, at.name AS asset_type, a.acquisition_date, a.acquisition_value, av.current_value " +
                    "FROM assets a " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "INNER JOIN asset_values av ON av.asset_id = a.asset_id AND av.valuation_date = ? " +
                    "WHERE a.user_id = ? ORDER BY a.name";

    private static final String SQL_ASSETS_WITH_LATEST =
            "SELECT a.name, at.name AS asset_type, a.acquisition_date, a.acquisition_value, av.current_value " +
                    "FROM assets a " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "LEFT JOIN LATERAL (SELECT current_value FROM asset_values av WHERE av.asset_id = a.asset_id ORDER BY valuation_date DESC LIMIT 1) av ON true " +
                    "WHERE a.user_id = ? ORDER BY a.name";

    private static final String SQL_LIABILITIES_WITH_VALUE_BY_DATE =
            "SELECT l.name, lt.name AS liability_type, l.principal_amount, i.annual_rate, l.start_date, lv.end_date, lv.outstanding_balance " +
                    "FROM liabilities l " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "INNER JOIN liability_values lv ON lv.liability_id = l.liability_id AND lv.valuation_date = ? " +
                    "LEFT JOIN interests i ON i.liability_id = l.liability_id AND i.start_date = l.start_date " +
                    "WHERE l.user_id = ? ORDER BY l.name";

    private static final String SQL_LIABILITIES_WITH_LATEST =
            "SELECT l.name, lt.name AS liability_type, l.principal_amount, i.annual_rate, l.start_date, lv.end_date, lv.outstanding_balance " +
                    "FROM liabilities l " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "LEFT JOIN interests i ON i.liability_id = l.liability_id AND i.start_date = l.start_date " +
                    "LEFT JOIN LATERAL (SELECT end_date, outstanding_balance FROM liability_values lv WHERE lv.liability_id = l.liability_id ORDER BY valuation_date DESC LIMIT 1) lv ON true " +
                    "WHERE l.user_id = ? ORDER BY l.name";

    private static final String SQL_ALL_TRANSACTIONS =
            "SELECT t.transaction_type, t.amount, t.transaction_date, c.name AS category, " +
                    "a.name AS asset, l.name AS liability, t.description " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category_id = c.category_id " +
                    "LEFT JOIN assets a ON t.asset_id = a.asset_id " +
                    "LEFT JOIN liabilities l ON t.liability_id = l.liability_id " +
                    "WHERE t.user_id = ? " +
                    "ORDER BY t.transaction_date DESC";


    private static final String SQL_ALL_ASSET_VALUES =
            "SELECT a.name AS asset_name, at.name AS asset_type, av.valuation_date, av.current_value " +
                    "FROM asset_values av " +
                    "JOIN assets a ON av.asset_id = a.asset_id " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "WHERE a.user_id = ? " +
                    "ORDER BY a.name, av.valuation_date DESC";

    private static final String SQL_ALL_LIABILITY_VALUES =
            "SELECT l.name AS liability_name, lt.name AS liability_type, lv.valuation_date, lv.outstanding_balance " +
                    "FROM liability_values lv " +
                    "JOIN liabilities l ON lv.liability_id = l.liability_id " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "WHERE l.user_id = ? " +
                    "ORDER BY l.name, lv.valuation_date DESC";

    public FinancialContextBuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public String buildContext(Long userId, LocalDate date) {
        StringBuilder context = new StringBuilder();
        
        // Encabezado limpio
        context.append("# SITUACIÓN FINANCIERA COMPLETA\n\n");
        
        // Resumen ejecutivo (valores actuales)
        appendSummary(context, userId);
        
        // Activos (valores actuales)
        appendAssets(context, userId, date);
        
        // Pasivos (valores actuales)
        appendLiabilities(context, userId, date);
        
        // Historial completo de activos
        appendAssetHistory(context, userId);
        
        // Historial completo de pasivos
        appendLiabilityHistory(context, userId);
        
        // Todas las transacciones
        appendAllTransactions(context, userId);
        
        return context.toString();
    }
    
    public String buildContextWithQuery(Long userId, LocalDate date, String userQuery) {
        StringBuilder context = new StringBuilder();
        
        // Encabezado limpio
        context.append("# SITUACIÓN FINANCIERA COMPLETA\n\n");
        
        // Resumen ejecutivo (valores actuales)
        appendSummary(context, userId);
        
        // Activos (valores actuales)
        appendAssets(context, userId, date);
        
        // Pasivos (valores actuales)
        appendLiabilities(context, userId, date);
        
        // Historial completo de activos
        appendAssetHistory(context, userId);
        
        // Historial completo de pasivos
        appendLiabilityHistory(context, userId);
        
        // Todas las transacciones
        appendAllTransactions(context, userId);
        
        return context.toString();
    }
    
    private void appendSummary(StringBuilder context, Long userId) {
        try {
            // Solo usar los valores MÁS RECIENTES de cada activo y pasivo
            String sql = "SELECT " +
                    "COALESCE((SELECT SUM(av.current_value) FROM asset_values av " +
                    "JOIN assets a ON av.asset_id = a.asset_id " +
                    "JOIN (SELECT asset_id, MAX(valuation_date) as max_date FROM asset_values GROUP BY asset_id) latest " +
                    "ON av.asset_id = latest.asset_id AND av.valuation_date = latest.max_date " +
                    "WHERE a.user_id = ?), 0) AS total_assets, " +
                    "COALESCE((SELECT SUM(lv.outstanding_balance) FROM liability_values lv " +
                    "JOIN liabilities l ON lv.liability_id = l.liability_id " +
                    "JOIN (SELECT liability_id, MAX(valuation_date) as max_date FROM liability_values GROUP BY liability_id) latest " +
                    "ON lv.liability_id = latest.liability_id AND lv.valuation_date = latest.max_date " +
                    "WHERE l.user_id = ?), 0) AS total_liabilities";
            
            Map<String, Object> totals = jdbcTemplate.queryForMap(sql, userId, userId);
            BigDecimal totalAssets = (BigDecimal) totals.get("total_assets");
            BigDecimal totalLiabilities = (BigDecimal) totals.get("total_liabilities");
            BigDecimal netWorth = totalAssets.subtract(totalLiabilities);
            
            context.append("## RESUMEN\n");
            context.append(String.format("- **Patrimonio Neto:** %s€\n", formatCurrency(netWorth)));
            context.append(String.format("- **Activos Totales:** %s€\n", formatCurrency(totalAssets)));
            context.append(String.format("- **Pasivos Totales:** %s€\n", formatCurrency(totalLiabilities)));
            context.append("\n");
            
        } catch (DataAccessException ex) {
            log.warn("Error calculando resumen: {}", ex.getMessage());
            context.append("## RESUMEN\n- No se pudieron calcular los totales\n\n");
        }
    }
    
    private void appendAssets(StringBuilder context, Long userId, LocalDate date) {
        context.append("## ACTIVOS\n");
        
        try {
            List<Map<String, Object>> assets;
            if (date != null) {
                assets = jdbcTemplate.queryForList(SQL_ASSETS_WITH_VALUE_BY_DATE, date, userId);
                context.append(String.format("*(Valores al %s)*\n", date.format(DATE_FORMATTER)));
            } else {
                assets = jdbcTemplate.queryForList(SQL_ASSETS_WITH_LATEST, userId);
                context.append("*(Últimos valores disponibles)*\n");
            }
            
            if (assets.isEmpty()) {
                context.append("- No hay activos registrados\n");
            } else {
                for (Map<String, Object> asset : assets) {
                    String name = safeString(asset.get("name"));
                    String type = safeString(asset.get("asset_type"));
                    BigDecimal acquisitionValue = (BigDecimal) asset.get("acquisition_value");
                    BigDecimal currentValue = (BigDecimal) asset.get("current_value");
                    LocalDate acquisitionDate = asset.get("acquisition_date") != null ? 
                            ((java.sql.Date) asset.get("acquisition_date")).toLocalDate() : null;
                    
                    context.append(String.format("- **%s** (%s)\n", name, type));
                    context.append(String.format("  - Valor actual: %s€\n", formatCurrency(currentValue)));
                    context.append(String.format("  - Valor adquisición: %s€\n", formatCurrency(acquisitionValue)));
                    if (acquisitionDate != null) {
                        context.append(String.format("  - Fecha adquisición: %s\n", acquisitionDate.format(DATE_FORMATTER)));
                    }
                    context.append("\n");
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando activos: {}", ex.getMessage());
            context.append("- Error al cargar activos\n");
        }
        
        context.append("\n");
    }
    
    private void appendLiabilities(StringBuilder context, Long userId, LocalDate date) {
        context.append("## PASIVOS\n");
        
        try {
            List<Map<String, Object>> liabilities;
            if (date != null) {
                liabilities = jdbcTemplate.queryForList(SQL_LIABILITIES_WITH_VALUE_BY_DATE, date, userId);
                context.append(String.format("*(Valores al %s)*\n", date.format(DATE_FORMATTER)));
            } else {
                liabilities = jdbcTemplate.queryForList(SQL_LIABILITIES_WITH_LATEST, userId);
                context.append("*(Últimos valores disponibles)*\n");
            }
            
            if (liabilities.isEmpty()) {
                context.append("- No hay pasivos registrados\n");
            } else {
                for (Map<String, Object> liability : liabilities) {
                    String name = safeString(liability.get("name"));
                    String type = safeString(liability.get("liability_type"));
                    BigDecimal principal = (BigDecimal) liability.get("principal_amount");
                    BigDecimal outstanding = (BigDecimal) liability.get("outstanding_balance");
                    BigDecimal rate = (BigDecimal) liability.get("annual_rate");
                    LocalDate startDate = liability.get("start_date") != null ? 
                            ((java.sql.Date) liability.get("start_date")).toLocalDate() : null;
                    LocalDate endDate = liability.get("end_date") != null ? 
                            ((java.sql.Date) liability.get("end_date")).toLocalDate() : null;
                    
                    context.append(String.format("- **%s** (%s)\n", name, type));
                    context.append(String.format("  - Saldo pendiente: %s€\n", formatCurrency(outstanding)));
                    context.append(String.format("  - Capital inicial: %s€\n", formatCurrency(principal)));
                    if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                        context.append(String.format("  - Tasa anual: %s%%\n", formatPercentage(rate)));
                    }
                    if (startDate != null) {
                        context.append(String.format("  - Inicio: %s\n", startDate.format(DATE_FORMATTER)));
                    }
                    if (endDate != null) {
                        context.append(String.format("  - Vencimiento: %s\n", endDate.format(DATE_FORMATTER)));
                    }
                    context.append("\n");
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando pasivos: {}", ex.getMessage());
            context.append("- Error al cargar pasivos\n");
        }
        
        context.append("\n");
    }
    
    
    
    
    
    
    
    
    
    private void formatTransaction(StringBuilder context, Map<String, Object> tx, DecimalFormat txFormat) {
        String type = safeString(tx.get("transaction_type"));
        BigDecimal amount = (BigDecimal) tx.get("amount");
        LocalDate txDate = tx.get("transaction_date") != null ?
                ((java.sql.Date) tx.get("transaction_date")).toLocalDate() : null;
        String category = safeString(tx.get("category"));
        String asset = safeString(tx.get("asset"));
        String liability = safeString(tx.get("liability"));
        String description = safeString(tx.get("description"));

        // Determinar etiqueta principal: categoría > descripción > vacío
        String label = !category.isEmpty() ? category : (!description.isEmpty() ? description : "");

        // Determinar cuenta a mostrar: asset primero, si no existe usar liability
        String account = !asset.isEmpty() ? asset : (!liability.isEmpty() ? liability : "");

        // Determinar signo y valor absoluto a mostrar
        boolean negative;
        if (amount != null) {
            negative = amount.signum() < 0;
        } else {
            negative = !"income".equalsIgnoreCase(type);
        }
        BigDecimal displayAmt = amount == null ? BigDecimal.ZERO : amount.abs();
        String amtFormatted = (negative ? "-" : "") + txFormat.format(displayAmt) + "€";

        String dateStr = txDate != null ? txDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";

        // Línea: yyyy-MM-dd | -62,85€ | Concepto [Cuenta]
        StringBuilder line = new StringBuilder();
        line.append(String.format("%s | %s | %s", dateStr, amtFormatted, label));
        if (!account.isEmpty()) {
            line.append(String.format(" [%s]", account));
        }
        context.append(line.toString()).append("\n");
    }
    
    private void appendAssetHistory(StringBuilder context, Long userId) {
        context.append("## HISTORIAL DE ACTIVOS\n");
        context.append("*(Valores históricos de todos los activos)*\n");
        
        try {
            List<Map<String, Object>> assetHistory = jdbcTemplate.queryForList(SQL_ALL_ASSET_VALUES, userId);
            
            if (assetHistory.isEmpty()) {
                context.append("- No hay historial de activos\n");
            } else {
                String currentAsset = "";
                for (Map<String, Object> record : assetHistory) {
                    String assetName = safeString(record.get("asset_name"));
                    String assetType = safeString(record.get("asset_type"));
                    LocalDate valuationDate = record.get("valuation_date") != null ? 
                            ((java.sql.Date) record.get("valuation_date")).toLocalDate() : null;
                    BigDecimal currentValue = (BigDecimal) record.get("current_value");
                    
                    // Agrupar por activo
                    if (!assetName.equals(currentAsset)) {
                        if (!currentAsset.isEmpty()) context.append("\n");
                        context.append(String.format("### %s (%s)\n", assetName, assetType));
                        currentAsset = assetName;
                    }
                    
                    if (valuationDate != null) {
                        context.append(String.format("- %s: %s€\n", 
                                valuationDate.format(DATE_FORMATTER), 
                                formatCurrency(currentValue)));
                    }
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando historial de activos: {}", ex.getMessage());
            context.append("- Error al cargar historial de activos\n");
        }
        
        context.append("\n");
    }
    
    private void appendLiabilityHistory(StringBuilder context, Long userId) {
        context.append("## HISTORIAL DE PASIVOS\n");
        context.append("*(Valores históricos de todos los pasivos)*\n");
        
        try {
            List<Map<String, Object>> liabilityHistory = jdbcTemplate.queryForList(SQL_ALL_LIABILITY_VALUES, userId);
            
            if (liabilityHistory.isEmpty()) {
                context.append("- No hay historial de pasivos\n");
            } else {
                String currentLiability = "";
                for (Map<String, Object> record : liabilityHistory) {
                    String liabilityName = safeString(record.get("liability_name"));
                    String liabilityType = safeString(record.get("liability_type"));
                    LocalDate valuationDate = record.get("valuation_date") != null ? 
                            ((java.sql.Date) record.get("valuation_date")).toLocalDate() : null;
                    BigDecimal outstandingBalance = (BigDecimal) record.get("outstanding_balance");
                    
                    // Agrupar por pasivo
                    if (!liabilityName.equals(currentLiability)) {
                        if (!currentLiability.isEmpty()) context.append("\n");
                        context.append(String.format("### %s (%s)\n", liabilityName, liabilityType));
                        currentLiability = liabilityName;
                    }
                    
                    if (valuationDate != null) {
                        context.append(String.format("- %s: %s€\n", 
                                valuationDate.format(DATE_FORMATTER), 
                                formatCurrency(outstandingBalance)));
                    }
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando historial de pasivos: {}", ex.getMessage());
            context.append("- Error al cargar historial de pasivos\n");
        }
        
        context.append("\n");
    }
    
    private void appendAllTransactions(StringBuilder context, Long userId) {
        context.append("## TODAS LAS TRANSACCIONES\n");
        context.append("*(Historial completo de transacciones)*\n");
        
        try {
            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(SQL_ALL_TRANSACTIONS, userId);
            
            if (transactions.isEmpty()) {
                context.append("- No hay transacciones registradas\n");
            } else {
                // Preparar formateador para moneda con coma decimal
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "ES"));
                symbols.setDecimalSeparator(',');
                symbols.setGroupingSeparator('.');
                DecimalFormat txFormat = new DecimalFormat("#,##0.00", symbols);

                for (Map<String, Object> tx : transactions) {
                    formatTransaction(context, tx,txFormat);
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Error consultando todas las transacciones: {}", ex.getMessage());
            context.append("- Error al cargar transacciones\n");
        }
        
        context.append("\n");
    }
    
    // Métodos utilitarios
    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }
    
    private String formatPercentage(BigDecimal rate) {
        if (rate == null) return "0.00";
        return String.format("%.2f", rate);
    }
}
