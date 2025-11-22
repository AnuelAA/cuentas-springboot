// language: java
package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ExcelNewServicePort;
import com.cuentas.backend.domain.File;
import com.cuentas.backend.domain.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Service
public class ExcelNewServiceUseCase implements ExcelNewServicePort {

    private static final Logger log = LoggerFactory.getLogger(ExcelNewServiceUseCase.class);
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    // =======================
    // Constantes SQL
    // =======================
    private static final String SQL_SELECT_ASSET_TYPE_ID = "SELECT asset_type_id FROM asset_types WHERE name = ?";
    private static final String SQL_SELECT_LIABILITY_TYPE_ID = "SELECT liability_type_id FROM liability_types WHERE name = ?";
    private static final String SQL_DELETE_TRANSACTIONS_YEAR =
            "DELETE FROM transactions WHERE user_id = ? AND transaction_date >= ? AND transaction_date <= ?";

    private static final String SQL_DELETE_ASSET_VALUES_YEAR =
            "DELETE FROM asset_values av USING assets a WHERE av.asset_id = a.asset_id AND a.user_id = ? AND av.valuation_date >= ? AND av.valuation_date <= ?";
    private static final String SQL_DELETE_LIABILITY_VALUES_YEAR =
            "DELETE FROM liability_values lv USING liabilities l WHERE lv.liability_id = l.liability_id AND l.user_id = ? AND lv.valuation_date >= ? AND lv.valuation_date <= ?";
    private static final String SQL_DELETE_INTEREST_HISTORY_YEAR =
            "DELETE FROM interest_history ih USING interests i, liabilities l " +
                    "WHERE ih.interest_id = i.interest_id AND i.liability_id = l.liability_id " +
                    "AND l.user_id = ? AND ((ih.start_date >= ? AND ih.start_date <= ?) OR (ih.end_date >= ? AND ih.end_date <= ?))";


    private static final String SQL_INSERT_CATEGORY =
            "INSERT INTO categories (user_id, name, created_at) VALUES (?, ?, NOW()) RETURNING category_id";
    private static final String SQL_SELECT_CATEGORY =
            "SELECT category_id FROM categories WHERE user_id = ? AND name = ?";

    private static final String SQL_INSERT_TRANSACTION =
            "INSERT INTO transactions (user_id, category_id, asset_id, liability_id, related_asset_id, transaction_type, amount, transaction_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_ASSET =
            "SELECT asset_id FROM assets WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_ASSET =
            "INSERT INTO assets (user_id, asset_type_id, name, acquisition_date, acquisition_value, created_at) VALUES (?, ?, ?, ?, ?, NOW()) RETURNING asset_id";
    private static final String SQL_UPDATE_ASSET =
            "UPDATE assets SET acquisition_date = ?, acquisition_value = ?, updated_at = NOW() WHERE asset_id = ?";

    private static final String SQL_SELECT_LIABILITY =
            "SELECT liability_id FROM liabilities WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_LIABILITY =
            "INSERT INTO liabilities (user_id, liability_type_id, name, principal_amount, start_date, created_at) VALUES (?, ?, ?, ?, ?, NOW()) RETURNING liability_id";
    private static final String SQL_UPDATE_LIABILITY =
            "UPDATE liabilities SET principal_amount = ?, start_date = ?, updated_at = NOW() WHERE liability_id = ?";

    private static final String SQL_INSERT_ASSET_VALUE =
            "INSERT INTO asset_values (asset_id, valuation_date, current_value, created_at) VALUES (?, ?, ?, NOW())";
    private static final String SQL_INSERT_LIABILITY_VALUE =
            "INSERT INTO liability_values (liability_id, valuation_date, end_date, outstanding_balance, created_at) VALUES (?, ?, ?, ?, NOW())";

    // interests
    private static final String SQL_SELECT_INTEREST =
            "SELECT interest_id FROM interests WHERE liability_id = ? AND start_date = ?";
    private static final String SQL_INSERT_INTEREST =
            "INSERT INTO interests (liability_id, type, annual_rate, start_date, created_at) VALUES (?, ?, ?, ?, NOW()) RETURNING interest_id";
    private static final String SQL_UPDATE_INTEREST =
            "UPDATE interests SET type = ?, annual_rate = ?, start_date = ?, created_at = NOW() WHERE interest_id = ?";

    // Exportación Excel

    private static final String sqlAssetsWithValue =
            "SELECT DISTINCT ON (a.asset_id) a.asset_id, a.name, at.name AS asset_type_name, a.acquisition_date, a.acquisition_value, av.current_value " +
                    "FROM assets a " +
                    "LEFT JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                    "INNER JOIN asset_values av ON av.asset_id = a.asset_id AND av.valuation_date <= ? " +
                    "WHERE a.user_id = ? " +
                    "ORDER BY a.asset_id, av.valuation_date DESC";

    private static final String sqlLiabsWithValue =
            "SELECT l.liability_id, l.name, lt.name AS liability_type_name, l.principal_amount, i.type AS interest_type, i.annual_rate, l.start_date, lv.end_date, lv.outstanding_balance " +
                    "FROM liabilities l " +
                    "LEFT JOIN liability_types lt ON l.liability_type_id = lt.liability_type_id " +
                    "INNER JOIN liability_values lv ON lv.liability_id = l.liability_id AND lv.valuation_date = ? " +
                    "LEFT JOIN interests i ON i.liability_id = l.liability_id AND i.start_date = l.start_date " +
                    "WHERE l.user_id = ? ORDER BY l.liability_id";

    private static final String sqlIncome =
            "SELECT t.category_id, c.name AS category_name, t.asset_id, a.name AS asset_name, " +
                    "t.liability_id, l.name AS liability_name, t.related_asset_id, ra.name AS related_asset_name, " +
                    "t.amount, t.transaction_date " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category_id = c.category_id " +
                    "LEFT JOIN assets a ON t.asset_id = a.asset_id " +
                    "LEFT JOIN liabilities l ON t.liability_id = l.liability_id " +
                    "LEFT JOIN assets ra ON t.related_asset_id = ra.asset_id " +
                    "WHERE t.user_id = ? AND t.transaction_type = 'income' AND DATE_TRUNC('month', t.transaction_date) = DATE_TRUNC('month', ?::date) " +
                    "ORDER BY t.transaction_id";

    private static final String sqlExpense =
            "SELECT t.category_id, c.name AS category_name, t.asset_id, a.name AS asset_name, " +
                    "t.liability_id, l.name AS liability_name, t.related_asset_id, ra.name AS related_asset_name, " +
                    "t.amount, t.transaction_date " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category_id = c.category_id " +
                    "LEFT JOIN assets a ON t.asset_id = a.asset_id " +
                    "LEFT JOIN liabilities l ON t.liability_id = l.liability_id " +
                    "LEFT JOIN assets ra ON t.related_asset_id = ra.asset_id " +
                    "WHERE t.user_id = ? AND t.transaction_type = 'expense' AND DATE_TRUNC('month', t.transaction_date) = DATE_TRUNC('month', ?::date) " +
                    "ORDER BY t.transaction_id";


    public ExcelNewServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void processExcel(File excelFile, int year, long userId) {
        byte[] data = excelFile.getFileData();
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Archivo vacío");
        }

        // Ajustar límite de seguridad de POI para permitir archivos con alta compresión
        ZipSecureFile.setMinInflateRatio(0.001);
        
        try (InputStream is = new ByteArrayInputStream(data);
             Workbook workbook = WorkbookFactory.create(is)) {

            log.info("Eliminando datos previos para user={} year={}", userId, year);
            deleteYearlyData(userId, year);

            List<String> months = Arrays.asList(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            );

            for (String monthName : months) {
                Sheet sheet = workbook.getSheet(monthName);
                if (sheet == null) continue;

                int month = getMonthNumber(monthName);
                LocalDate defaultDate = LocalDate.of(year, month, 1);

                log.info("Procesando hoja [{}] para usuario {} en año {}", monthName, userId, year);

                // primero leemos y guardamos los activos y pasivos (y sus valores mensuales)
                // Activos: T(nombre), U(tipo), V(fecha adquisición), W(valor adquisición), X(valor actual)
                int assetCount = saveAssets(sheet, userId, defaultDate, "T", "U", "V", "W","X");
                // Pasivos: AA(nombre), AB(tipo), AC(cantidad inicial), AD(tasa interés), AE(fecha inicio), AF(fecha fin), AG(saldo pendiente)
                int liabilityCount = saveLiabilities(sheet, userId, defaultDate, "AA", "AB", "AC", "AD", "AE", "AF", "AG");

                // luego ingresos y gastos
                List<Transaction> incomeTransactionList = saveIncome(sheet, userId, "F", "G", "H", "I", "J", "K", defaultDate);
                List<Transaction> expenseTransactionList = saveExpense(sheet, userId, "M", "N", "O", "P", "Q", "R", defaultDate);

                log.info("Filas leídas - Ingresos: {}, Gastos: {}, Activos: {}, Pasivos: {}",
                        incomeTransactionList.size(), expenseTransactionList.size(), assetCount, liabilityCount);
            }

        } catch (Exception e) {
            log.error("Error procesando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando Excel", e);
        }
    }

    // =======================
    // Lectura Excel y guardado en BD
    // =======================
    private int saveAssets(Sheet sheet, Long userId, LocalDate valuationDate, String colCategory, String colType, String colAcqDate,
                           String AcqValue, String colCurrentValue) {
        int processed = 0;
        int start = 4; // fila 5 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell typeCell = row.getCell(CellReference.convertColStringToIndex(colType));
            Cell acqDateCell = row.getCell(CellReference.convertColStringToIndex(colAcqDate));
            Cell acqValueCell = row.getCell(CellReference.convertColStringToIndex(AcqValue));
            Cell currentValueCell = row.getCell(CellReference.convertColStringToIndex(colCurrentValue));

            if (catCell == null || catCell.toString().isBlank() ||
                    currentValueCell == null || currentValueCell.toString().isBlank()
                    || typeCell == null || typeCell.toString().isBlank()) continue;

            String name = catCell.toString();
            Long assetTypeId = getAssetTypeId(typeCell.toString());
            LocalDate acqDate = parseDateCell(acqDateCell);
            Double acqValue = acqValueCell != null ? evaluaSumaResta(acqValueCell.toString()) : 0D;
            Double currentValue = evaluaSumaResta(currentValueCell.toString());
            //1.crear/actualizar asset
            Long assetId = upsertAsset(userId, assetTypeId, name, acqDate, acqValue);
            //2.Crear histórico mensual de asset_values
            if (assetId != null) {
                insertAssetValue(assetId, valuationDate, currentValue);
            }

            processed++;
        }
        return processed;
    }

    private int saveLiabilities(Sheet sheet, Long userId, LocalDate valuationDate, String colCategory, String colType,
                                String colPriAmount, String colInterestRate, String colStartDate,
                                String colEndDate, String colOutstandingBalance) {
        int processed = 0;
        int start = 4; // fila 5 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell typeCell = row.getCell(CellReference.convertColStringToIndex(colType));
            Cell priAmountCell = row.getCell(CellReference.convertColStringToIndex(colPriAmount));
            Cell interestRateCell = row.getCell(CellReference.convertColStringToIndex(colInterestRate));
            Cell startDateCell = row.getCell(CellReference.convertColStringToIndex(colStartDate));
            Cell endDateCell = row.getCell(CellReference.convertColStringToIndex(colEndDate));
            Cell outstandingBalanceCell = row.getCell(CellReference.convertColStringToIndex(colOutstandingBalance));

            if (catCell == null || catCell.toString().isBlank() ||
                    outstandingBalanceCell == null || outstandingBalanceCell.toString().isBlank()
                    || typeCell == null || typeCell.toString().isBlank()) continue;

            String name = catCell.toString();
            Long liabilityTypeId = getLiabilityTypeId(typeCell.toString());
            Double priAmount = priAmountCell != null ? evaluaSumaResta(priAmountCell.toString()) : 0D;
            BigDecimal interestRate = interestRateCell != null && !interestRateCell.toString().isBlank()
                    ? new BigDecimal(interestRateCell.toString().replace(",", "."))
                    : null;
            LocalDate startDate = parseDateCell(startDateCell);
            LocalDate endDate = parseDateCell(endDateCell);
            var outstandingBalance = evaluaSumaResta(outstandingBalanceCell.toString());

            //1.crear/actualizar liability
            Long liabilityId = upsertLiability(userId, liabilityTypeId, name, priAmount, startDate);
            //2.crear/actualizar el interes del liability (si hay interestRate)
            if (liabilityId != null) {
                upsertInterest(liabilityId, interestRate, startDate);

                //3.Crear histórico mensual de liability_values
                insertLiabilityValue(liabilityId, valuationDate, endDate, outstandingBalance);
            }
            processed++;
        }
        return processed;
    }

    private List<Transaction> saveIncome(Sheet sheet, Long userId, String colCategory, String colAsset,
                                         String colLiability, String colRelatedAsset, String colAmount, String colDate, LocalDate defaultDate) {
        List<Transaction> transactionList = new ArrayList<>();
        int start = 2; // fila 3 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell assetCell = row.getCell(CellReference.convertColStringToIndex(colAsset));
            Cell liabilityCell = row.getCell(CellReference.convertColStringToIndex(colLiability));
            Cell relatedAssetCell = row.getCell(CellReference.convertColStringToIndex(colRelatedAsset));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));
            Cell dateCell = row.getCell(CellReference.convertColStringToIndex(colDate));

            if (catCell == null || catCell.toString().isBlank() ||
                    amountCell == null || amountCell.toString().isBlank()) continue;

            Long categoryId = getCategoryId(userId, catCell.toString());
            Long assetId = assetCell != null && !assetCell.toString().isBlank() ? getAssetId(userId, assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && !liabilityCell.toString().isBlank() ? getLiabilityId(userId, liabilityCell.toString()) : null;
            Long relatedAssetId = relatedAssetCell != null && !relatedAssetCell.toString().isBlank() ? getAssetId(userId, relatedAssetCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());
            
            // Intentar parsear fecha de la celda, si no usar defaultDate
            LocalDate transactionDate = defaultDate;
            if (dateCell != null && !dateCell.toString().trim().isEmpty()) {
                LocalDate parsedDate = parseDateCell(dateCell);
                if (parsedDate != null) {
                    transactionDate = parsedDate;
                }
            }

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, relatedAssetId, "income", amount, transactionDate);

            transactionList.add(new Transaction(userId, categoryId, assetId, liabilityId, relatedAssetId, amount, "income", transactionDate));
        }
        return transactionList;
    }


    private List<Transaction> saveExpense(Sheet sheet, Long userId, String colCategory, String colAsset,
                                          String colLiability, String colRelatedAsset, String colAmount, String colDate, LocalDate defaultDate) {
        List<Transaction> transactionList = new ArrayList<>();
        int start = 2; // fila 3 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell assetCell = row.getCell(CellReference.convertColStringToIndex(colAsset));
            Cell liabilityCell = row.getCell(CellReference.convertColStringToIndex(colLiability));
            Cell relatedAssetCell = row.getCell(CellReference.convertColStringToIndex(colRelatedAsset));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));
            Cell dateCell = row.getCell(CellReference.convertColStringToIndex(colDate));

            if (catCell == null || catCell.toString().isBlank() ||
                    amountCell == null || amountCell.toString().isBlank()) continue;

            Long categoryId = getCategoryId(userId, catCell.toString());
            Long assetId = assetCell != null && !assetCell.toString().isBlank() ? getAssetId(userId, assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && !liabilityCell.toString().isBlank() ? getLiabilityId(userId, liabilityCell.toString()) : null;
            Long relatedAssetId = relatedAssetCell != null && !relatedAssetCell.toString().isBlank() ? getAssetId(userId, relatedAssetCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());
            
            // Intentar parsear fecha de la celda, si no usar defaultDate
            LocalDate transactionDate = defaultDate;
            if (dateCell != null && !dateCell.toString().trim().isEmpty()) {
                LocalDate parsedDate = parseDateCell(dateCell);
                if (parsedDate != null) {
                    transactionDate = parsedDate;
                }
            }

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, relatedAssetId, "expense", amount, transactionDate);

            transactionList.add(new Transaction(userId, categoryId, assetId, liabilityId, relatedAssetId, amount, "expense", transactionDate));
        }
        return transactionList;
    }


    // =======================
    // Utilidades
    // =======================

    private void deleteYearlyData(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        int deletedTx = jdbcTemplate.update(SQL_DELETE_TRANSACTIONS_YEAR, userId, start, end);
        int deletedAssetValues = jdbcTemplate.update(SQL_DELETE_ASSET_VALUES_YEAR, userId, start, end);
        int deletedLiabilityValues = jdbcTemplate.update(SQL_DELETE_LIABILITY_VALUES_YEAR, userId, start, end);
        int deletedInterestHistory = jdbcTemplate.update(SQL_DELETE_INTEREST_HISTORY_YEAR, userId, start, end, start, end);
        log.info("Eliminados - transacciones: {}, asset_values: {}, liability_values: {}, interest_history: {} for user={}, year={}",
                deletedTx, deletedAssetValues, deletedLiabilityValues, deletedInterestHistory, userId, year);
    }

    private Long upsertLiability(Long userId, Long liabilityTypeId, String name, Double priAmount,
                                 LocalDate startDate) {
        try {
            // Buscar si existe la liability
            Long liabilityId = jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY, Long.class, userId, name);
            // Si existe, actualizar
            jdbcTemplate.update(SQL_UPDATE_LIABILITY, priAmount, startDate, liabilityId);
            return liabilityId;
        } catch (DataAccessException e) {
            return jdbcTemplate.queryForObject(SQL_INSERT_LIABILITY, Long.class, userId, liabilityTypeId, name, priAmount,
                    startDate);
        }
    }

    private Long upsertAsset(Long userId, Long assetTypeId, String name, LocalDate acqDate, Double acqValue) {
        try {
            // Buscar si existe el asset
            Long assetId = jdbcTemplate.queryForObject(SQL_SELECT_ASSET, Long.class, userId, name);
            // Si existe, actualizar
            jdbcTemplate.update(SQL_UPDATE_ASSET, acqDate, acqValue, assetId);
            return assetId;
        } catch (DataAccessException e) {
            // Si no existe, insertar
            return jdbcTemplate.queryForObject(SQL_INSERT_ASSET, Long.class, userId, assetTypeId, name, acqDate, acqValue);
        }
    }

    private void insertAssetValue(Long assetId, LocalDate valuationDate, Double currentValue) {
        jdbcTemplate.update(SQL_INSERT_ASSET_VALUE, assetId, valuationDate, currentValue);
    }

    private void insertLiabilityValue(Long liabilityId, LocalDate valuationDate, LocalDate endDate, Double outstandingBalance) {
        jdbcTemplate.update(SQL_INSERT_LIABILITY_VALUE, liabilityId, valuationDate, endDate, outstandingBalance);
    }

    private Long upsertInterest(Long liabilityId, BigDecimal annualRate, LocalDate startDate) {
        // Si startDate es null, usar fecha actual como start
        LocalDate sDate = startDate != null ? startDate : LocalDate.now();
        try {
            Long interestId = jdbcTemplate.queryForObject(SQL_SELECT_INTEREST, Long.class, liabilityId, sDate);
            // actualizar
            jdbcTemplate.update(SQL_UPDATE_INTEREST, "fixed", annualRate, sDate, interestId);
            return interestId;
        } catch (DataAccessException e) {
            // insertar
            return jdbcTemplate.queryForObject(SQL_INSERT_INTEREST, Long.class, liabilityId, "fixed", annualRate, sDate);
        }
    }

    private Long getAssetTypeId(String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_ASSET_TYPE_ID, Long.class, name);
    }

    private Long getAssetId(Long userId, String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_ASSET, Long.class, userId, name);
    }

    private Long getLiabilityTypeId(String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY_TYPE_ID, Long.class, name);
    }

    /**
     * Parsea una celda de Excel que puede contener una fecha en formato numérico (Date) o texto (String).
     * Maneja múltiples formatos de fecha.
     */
    private LocalDate parseDateCell(Cell dateCell) {
        if (dateCell == null) {
            return null;
        }

        try {
            String dateString = null;
            CellType cellType = dateCell.getCellType();
            
            // Si la celda es de tipo NUMERIC, puede ser una fecha de Excel
            if (cellType == CellType.NUMERIC) {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(dateCell)) {
                    // Es una fecha numérica formateada de Excel
                    try {
                        java.util.Date date = dateCell.getDateCellValue();
                        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    } catch (Exception e) {
                        log.warn("Error convirtiendo fecha numérica, intentando como texto: {}", e.getMessage());
                        // Fallback: intentar obtener el valor formateado como string
                        DataFormatter formatter = new DataFormatter();
                        dateString = formatter.formatCellValue(dateCell).trim();
                    }
                } else {
                    // No está formateada como fecha, no es una fecha
                    return null;
                }
            } 
            // Si es STRING, obtener el valor directamente
            else if (cellType == CellType.STRING) {
                dateString = dateCell.getStringCellValue().trim();
            } 
            // Si es FORMULA, evaluar primero
            else if (cellType == CellType.FORMULA) {
                // Intentar obtener el valor como fecha si es numérico
                if (dateCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(dateCell)) {
                        try {
                            java.util.Date date = dateCell.getDateCellValue();
                            return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        } catch (Exception e) {
                            DataFormatter formatter = new DataFormatter();
                            dateString = formatter.formatCellValue(dateCell).trim();
                        }
                    }
                } else if (dateCell.getCachedFormulaResultType() == CellType.STRING) {
                    dateString = dateCell.getStringCellValue().trim();
                }
            }

            // Si tenemos un string para parsear
            if (dateString == null || dateString.isBlank()) {
                return null;
            }

            // Log del valor que intentamos parsear para debugging
            log.debug("Intentando parsear fecha: '{}'", dateString);

            // Intentar parsear con el formato esperado (dd-MMM-yyyy con Locale.ENGLISH)
            try {
                return LocalDate.parse(dateString, DATE_FORMATTER);
            } catch (Exception e) {
                log.debug("Fallo parseo con dd-MMM-yyyy para '{}': {}", dateString, e.getMessage());
                
                // Intentar otros formatos comunes
                String[] formats = {
                    "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
                    "dd-MM-yyyy", "MM-dd-yyyy", "dd.MM.yyyy",
                    "d-MMM-yyyy", "dd MMM yyyy", "d MMM yyyy"
                };
                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);
                        return LocalDate.parse(dateString, formatter);
                    } catch (Exception ignored) {}
                }
                
                log.error("No se pudo parsear la fecha: '{}'. Tipo de celda: {}", dateString, cellType);
                return null;
            }
        } catch (Exception e) {
            log.error("Error parseando celda de fecha (tipo: {}): {}", dateCell.getCellType(), e.getMessage(), e);
            return null;
        }
    }

    private Long getLiabilityId(Long userId, String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY, Long.class, userId, name);
    }

    private Long getCategoryId(Long userId, String name) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_CATEGORY, Long.class, userId, name);
        } catch (DataAccessException e) {
            // No existe, lo creamos
            return jdbcTemplate.queryForObject(SQL_INSERT_CATEGORY, Long.class, userId, name);
        }
    }

    private int getMonthNumber(String monthName) {
        return switch (monthName) {
            case "Enero" -> 1;
            case "Febrero" -> 2;
            case "Marzo" -> 3;
            case "Abril" -> 4;
            case "Mayo" -> 5;
            case "Junio" -> 6;
            case "Julio" -> 7;
            case "Agosto" -> 8;
            case "Septiembre" -> 9;
            case "Octubre" -> 10;
            case "Noviembre" -> 11;
            case "Diciembre" -> 12;
            default -> 0;
        };
    }

    private Double evaluaSumaResta(String expr) {
        if (expr == null || expr.isBlank()) return 0D;
        expr = expr.replace(",", ".").replaceAll("[^0-9+\\-\\.]", "");
        try {
            Double resultado = 0D;
            String[] tokens = expr.split("(?=[+-])");
            for (String token : tokens) {
                resultado += Double.parseDouble(token.trim());
            }
            return resultado;
        } catch (Exception e) {
            return 0D;
        }
    }

    @Override
    public byte[] exportExcel(int year, long userId) {
        List<String> months = Arrays.asList(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        );

        DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int m = 1; m <= 12; m++) {
                LocalDate firstOfMonth = LocalDate.of(year, m, 1);
                String monthName = months.get(m - 1);
                Sheet sheet = workbook.createSheet(monthName);

                // Crear 102 filas para garantizar la misma estructura que processExcel (índices 0..101)
                for (int r = 0; r < 102; r++) {
                    if (sheet.getRow(r) == null) sheet.createRow(r);
                }

                // Consultas por mes (reutilizan las constantes ya definidas)
                List<Map<String, Object>> incomes = jdbcTemplate.queryForList(sqlIncome, userId, firstOfMonth);
                List<Map<String, Object>> expenses = jdbcTemplate.queryForList(sqlExpense, userId, firstOfMonth);

                // Escribir transacciones (fila inicial 3 => índice 2)
                int txStartRow = 2;
                int maxTxRows = Math.max(incomes.size(), expenses.size());
                for (int i = 0; i < maxTxRows; i++) {
                    Row row = sheet.getRow(txStartRow + i);
                    if (row == null) row = sheet.createRow(txStartRow + i);

                    if (i < incomes.size()) {
                        Map<String, Object> inc = incomes.get(i);
                        if (inc.get("category_name") != null)
                            row.createCell(5).setCellValue(inc.get("category_name").toString());
                        if (inc.get("asset_name") != null)
                            row.createCell(6).setCellValue(inc.get("asset_name").toString());
                        if (inc.get("liability_name") != null)
                            row.createCell(7).setCellValue(inc.get("liability_name").toString());
                        if (inc.get("related_asset_name") != null)
                            row.createCell(8).setCellValue(inc.get("related_asset_name").toString());
                        if (inc.get("amount") != null)
                            row.createCell(9).setCellValue(((Number) inc.get("amount")).doubleValue());
                        // Fecha
                        if (inc.get("transaction_date") != null) {
                            Object dateObj = inc.get("transaction_date");
                            String s = dateObj.toString();
                            try {
                                if (dateObj instanceof java.sql.Date) {
                                    s = ((java.sql.Date) dateObj).toLocalDate().format(dtf);
                                } else if (dateObj instanceof java.time.LocalDate) {
                                    s = ((java.time.LocalDate) dateObj).format(dtf);
                                } else {
                                    s = LocalDate.parse(dateObj.toString()).format(dtf);
                                }
                            } catch (Exception ignored) {}
                            row.createCell(10).setCellValue(s);
                        }
                    }

                    if (i < expenses.size()) {
                        Map<String, Object> exp = expenses.get(i);
                        if (exp.get("category_name") != null)
                            row.createCell(12).setCellValue(exp.get("category_name").toString());
                        if (exp.get("asset_name") != null)
                            row.createCell(13).setCellValue(exp.get("asset_name").toString());
                        if (exp.get("liability_name") != null)
                            row.createCell(14).setCellValue(exp.get("liability_name").toString());
                        if (exp.get("related_asset_name") != null)
                            row.createCell(15).setCellValue(exp.get("related_asset_name").toString());
                        if (exp.get("amount") != null)
                            row.createCell(16).setCellValue(((Number) exp.get("amount")).doubleValue());
                        // Fecha
                        if (exp.get("transaction_date") != null) {
                            Object dateObj = exp.get("transaction_date");
                            String s = dateObj.toString();
                            try {
                                if (dateObj instanceof java.sql.Date) {
                                    s = ((java.sql.Date) dateObj).toLocalDate().format(dtf);
                                } else if (dateObj instanceof java.time.LocalDate) {
                                    s = ((java.time.LocalDate) dateObj).format(dtf);
                                } else {
                                    s = LocalDate.parse(dateObj.toString()).format(dtf);
                                }
                            } catch (Exception ignored) {}
                            row.createCell(17).setCellValue(s);
                        }
                    }
                }

                // Assets (fila inicial 5 => índice 4) en columnas S..X (19..24)
                List<Map<String, Object>> assets = jdbcTemplate.queryForList(sqlAssetsWithValue, firstOfMonth, userId);
                int assetRow = 4;
                LocalDate prevDate = firstOfMonth.minusMonths(1);
                for (Map<String, Object> r : assets) {
                    Row row = sheet.getRow(assetRow);
                    if (row == null) row = sheet.createRow(assetRow);
                    Long assetId = r.get("asset_id") != null ? ((Number) r.get("asset_id")).longValue() : null;
                    // name
                    row.createCell(19).setCellValue(r.get("name") != null ? r.get("name").toString() : "");
                    row.createCell(20).setCellValue(r.get("asset_type_name") != null ? r.get("asset_type_name").toString() : "");
                    // acquisition_date
                    if (r.get("acquisition_date") != null) {
                        Object d = r.get("acquisition_date");
                        String s = d.toString();
                        try {
                            if (d instanceof java.sql.Date) {
                                s = ((java.sql.Date) d).toLocalDate().format(dtf);
                            } else if (d instanceof java.time.LocalDate) {
                                s = ((java.time.LocalDate) d).format(dtf);
                            } else {
                                s = LocalDate.parse(d.toString()).format(dtf);
                            }
                        } catch (Exception ignored) {}
                        row.createCell(21).setCellValue(s);
                    } else {
                        row.createCell(21).setCellValue("");
                    }
                    // acquisition_value
                    double acqVal = r.get("acquisition_value") != null ? ((Number) r.get("acquisition_value")).doubleValue() : 0.0;
                    row.createCell(22).setCellValue(acqVal);
                    // current_value (valor del mes)
                    double currentValue = r.get("current_value") != null ? ((Number) r.get("current_value")).doubleValue() : 0.0;
                    row.createCell(23).setCellValue(currentValue);

                    // respecto al mes anterior -> columna Y (índice 24)
                    Double prev = assetId != null ? getPreviousAssetValue(assetId, prevDate) : null;
                    if (prev != null) {
                        row.createCell(24).setCellValue(currentValue - prev);
                    } else {
                        // si no hay dato anterior, dejar 0 o vacío; aquí se pone 0.0
                        row.createCell(24).setCellValue(0.0);
                    }

                    assetRow++;
                    if (assetRow >= 102) break;
                }

                // Liabilities (fila inicial 5 => índice 4) en columnas Z..AF (26..33)
                List<Map<String, Object>> liabs = jdbcTemplate.queryForList(sqlLiabsWithValue, firstOfMonth, userId);
                int liabRow = 4;
                for (Map<String, Object> r : liabs) {
                    Row row = sheet.getRow(liabRow);
                    if (row == null) row = sheet.createRow(liabRow);
                    Long liabilityId = r.get("liability_id") != null ? ((Number) r.get("liability_id")).longValue() : null;
                    row.createCell(26).setCellValue(r.get("name") != null ? r.get("name").toString() : "");
                    row.createCell(27).setCellValue(r.get("liability_type_name") != null ? r.get("liability_type_name").toString() : "");
                    row.createCell(28).setCellValue(r.get("principal_amount") != null ? ((Number) r.get("principal_amount")).doubleValue() : 0.0);
                    row.createCell(29).setCellValue(r.get("annual_rate") != null ? ((Number) r.get("annual_rate")).doubleValue() : 0.0);
                    // start_date
                    if (r.get("start_date") != null) {
                        Object d = r.get("start_date");
                        String s = d.toString();
                        try {
                            if (d instanceof java.sql.Date) {
                                s = ((java.sql.Date) d).toLocalDate().format(dtf);
                            } else if (d instanceof java.time.LocalDate) {
                                s = ((java.time.LocalDate) d).format(dtf);
                            } else {
                                s = LocalDate.parse(d.toString()).format(dtf);
                            }
                        } catch (Exception ignored) {}
                        row.createCell(30).setCellValue(s);
                    } else {
                        row.createCell(30).setCellValue("");
                    }
                    // end_date
                    if (r.get("end_date") != null) {
                        Object d = r.get("end_date");
                        String s = d.toString();
                        try {
                            if (d instanceof java.sql.Date) {
                                s = ((java.sql.Date) d).toLocalDate().format(dtf);
                            } else if (d instanceof java.time.LocalDate) {
                                s = ((java.time.LocalDate) d).format(dtf);
                            } else {
                                s = LocalDate.parse(d.toString()).format(dtf);
                            }
                        } catch (Exception ignored) {}
                        row.createCell(31).setCellValue(s);
                    } else {
                        row.createCell(31).setCellValue("");
                    }
                    double outstanding = r.get("outstanding_balance") != null ? ((Number) r.get("outstanding_balance")).doubleValue() : 0.0;
                    row.createCell(32).setCellValue(outstanding);

                    // respecto al mes anterior -> columna AG (índice 33)
                    Double prevOutstanding = liabilityId != null ? getPreviousLiabilityOutstanding(liabilityId, prevDate) : null;
                    if (prevOutstanding != null) {
                        row.createCell(33).setCellValue(outstanding - prevOutstanding);
                    } else {
                        row.createCell(33).setCellValue(0.0);
                    }

                    liabRow++;
                    if (liabRow >= 102) break;
                }
                writeHeaders(sheet, workbook,userId, firstOfMonth);
                aplicarFormatosMonedaYFecha(sheet, workbook);
            }
            autoSizeColumn(workbook);
            createBorders(workbook);
            aplicarFormulas(workbook);
            workbook.write(out);
            // cerrar workbook para liberar recursos y asegurar integridad
            workbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel export estructurado por meses para userId={}, year={}: {}", userId, year, e.getMessage(), e);
            throw new RuntimeException("Error generando Excel export", e);
        }
    }


    private static void autoSizeColumn(XSSFWorkbook workbook) {
        int lastCol = 33; // A..AH
        int emptyColumnChars = 4;   // ancho para columnas vacías (en caracteres)
        int minContentChars = 6;    // ancho mínimo para columnas con contenido
        DataFormatter formatter = new DataFormatter();

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            int lastRow = sheet.getLastRowNum();
            for (int c = 0; c <= lastCol; c++) {
                // auto size basado en contenido visible
                try {
                    sheet.autoSizeColumn(c);
                } catch (Exception ignored) {
                    // en casos raros (SXSSF o content expulsado) fallback y continuar
                }

                // detectar si la columna está realmente vacía (ninguna celda con texto/número)
                boolean hasContent = false;
                for (int r = 0; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String text = formatter.formatCellValue(cell);
                    if (text != null && !text.isBlank()) {
                        hasContent = true;
                        break;
                    }
                }

                if (!hasContent) {
                    // columna vacía: ponerla muy estrecha
                    sheet.setColumnWidth(c, emptyColumnChars * 256);
                } else {
                    // columna con contenido: asegurar un ancho mínimo legible
                    int current = sheet.getColumnWidth(c);
                    int minWidth = minContentChars * 256;
                    if (current < minWidth) {
                        sheet.setColumnWidth(c, minWidth);
                    }
                }
            }
        }
    }

    // language: java
    private static void createBorders(Workbook workbook) {
        String[] columns = {
                "F","G","H","I","J","K","M","N","O","P","Q","R",
                "S","T","U","V","W","X","Y","Z",
                "AB","AC","AD","AE","AF","AG","AH"
        };

        final int startRow = 1;  // fila 2 en Excel
        final int endRow = 51;   // fila 52 en Excel

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            for (String col : columns) {
                int colIdx = CellReference.convertColStringToIndex(col);
                for (int rowIdx = startRow; rowIdx <= endRow; rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;
                    Cell cell = row.getCell(colIdx);
                    if (cell == null) cell = row.createCell(colIdx);

                    CellStyle originalStyle = cell.getCellStyle();
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(originalStyle);

                    // Borde izquierdo y derecho grueso para todas las filas del rango
                    newStyle.setBorderLeft(BorderStyle.THICK);
                    newStyle.setBorderRight(BorderStyle.THICK);

                    // Top grueso solo en la primera fila del rango, thin en las demás
                    if (rowIdx == startRow) {
                        newStyle.setBorderTop(BorderStyle.THICK);
                    } else {
                        newStyle.setBorderTop(BorderStyle.THIN);
                    }

                    // Bottom grueso solo en la última fila del rango, thin en las demás
                    if (rowIdx == endRow) {
                        newStyle.setBorderBottom(BorderStyle.THICK);
                    } else {
                        newStyle.setBorderBottom(BorderStyle.THIN);
                    }

                    cell.setCellStyle(newStyle);
                }
            }
        }
    }

    private void writeHeaders(Sheet sheet, Workbook workbook, long userId, LocalDate firstOfMonth) {
        writeHeaderRow2(sheet, workbook);
        writeHeaderRow3(sheet, workbook);
        writeHeaderRow4(sheet, workbook);

        writeHeaderLeft(sheet, workbook, userId, firstOfMonth);
    }

    private void writeHeaderLeft(Sheet sheet, Workbook workbook, long userId, LocalDate firstOfMonth) {
        //Neto (fila 2 => índice 1) - Beneficio: Ingresos - Gastos
        Row header = sheet.getRow(1);
        createCellWithFormat(workbook, header.createCell(1), "Neto", IndexedColors.YELLOW, false);
        String netoFormula = "=" + sumaRango("J3","J52").substring(1) + "-" + sumaRango("Q3","Q52").substring(1);
        createCellWithFormat(workbook, header.createCell(2), netoFormula, IndexedColors.LIGHT_YELLOW, false);

        //Beneficio (fila 3 => índice 2)
        header = sheet.getRow(2);
        createCellWithFormat(workbook, header.createCell(1), "Beneficio", IndexedColors.YELLOW, false);
        String beneficioFormula = "=" + sumaRango("J3","J52").substring(1) + "-" + sumaRango("Q3","Q52").substring(1);
        createCellWithFormat(workbook, header.createCell(2), beneficioFormula, IndexedColors.LIGHT_YELLOW, false);        //Ingreso Neto (fila 4 => índice 3)
        header = sheet.getRow(3);
        createCellWithFormat(workbook, header.createCell(1), "Ingreso Neto", IndexedColors.YELLOW, false);
        createCellWithFormat(workbook, header.createCell(2), sumaRango("J3","J52"), IndexedColors.LIGHT_YELLOW, false);

        //Tesoreria neta (fila 6 => índice 5)
        header = sheet.getRow(5);
        createCellWithFormat(workbook, header.createCell(1), "Tesoreria neta", IndexedColors.YELLOW, false);
        String tesoreriaFormula = "=" + sumaRango("X5","X52").substring(1) + "-" + sumaRango("AG5","AG52").substring(1);
        createCellWithFormat(workbook, header.createCell(2), tesoreriaFormula, IndexedColors.LIGHT_YELLOW, false);
        //Total Líquido (fila 7 => índice 6)
        header = sheet.getRow(6);
        createCellWithFormat(workbook, header.createCell(1), "Total Líquido", IndexedColors.YELLOW, false);
        createCellWithFormat(workbook, header.createCell(2), String.valueOf(sumCuentaBancariaCurrentMonth(userId, firstOfMonth)), IndexedColors.LIGHT_YELLOW, false);

        //Total Invertido (fila 8 => índice 7)
        header = sheet.getRow(7);
        createCellWithFormat(workbook, header.createCell(1), "Total Invertido", IndexedColors.YELLOW, false);
        createCellWithFormat(workbook, header.createCell(2), String.valueOf(sumFondoInversionCurrentMonth(userId, firstOfMonth)), IndexedColors.LIGHT_YELLOW, false);
    }

    private static void writeHeaderRow2(Sheet sheet, Workbook workbook) {
        Row header = sheet.getRow(1);
        createCellWithFormat(workbook, header.createCell(5), "Ingresos", IndexedColors.GREEN, false);
        createCellWithFormat(workbook, header.createCell(6), "Activo", IndexedColors.GREEN, false);
        createCellWithFormat(workbook, header.createCell(7), "Pasivo", IndexedColors.GREEN, false);
        createCellWithFormat(workbook, header.createCell(8), "Activo que repercute", IndexedColors.GREEN, false);
        createCellWithFormat(workbook, header.createCell(9), sumaRango("J3","J52"), IndexedColors.GREEN, false);
        createCellWithFormat(workbook, header.createCell(10), "Fecha", IndexedColors.GREEN, false);

        createCellWithFormat(workbook, header.createCell(12), "Gastos", IndexedColors.RED, false);
        createCellWithFormat(workbook, header.createCell(13), "Activo", IndexedColors.RED, false);
        createCellWithFormat(workbook, header.createCell(14), "Pasivo", IndexedColors.RED, false);
        createCellWithFormat(workbook, header.createCell(15), "Activo que repercute", IndexedColors.RED, false);
        createCellWithFormat(workbook, header.createCell(16), sumaRango("Q3","Q52"), IndexedColors.RED, false);
        createCellWithFormat(workbook, header.createCell(17), "Fecha", IndexedColors.RED, false);

        createCellWithFormat(workbook, header.createCell(19), "ACTIVOS", IndexedColors.LIME, true);
        sheet.addMergedRegion(new CellRangeAddress(1,1,19,24)); // Agrupa S..X
        createCellWithFormat(workbook, header.createCell(26), "PASIVOS", IndexedColors.GOLD, true);
        sheet.addMergedRegion(new CellRangeAddress(1,1,26,33)); // Agrupa Z..AG
    }

    private static void writeHeaderRow3(Sheet sheet, Workbook workbook) {
        Row header = sheet.getRow(2);
        //Activos
        createCellWithFormat(workbook, header.createCell(19), "Concepto", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(20), "Tipo", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(21), "Fecha adquisión", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(22), "Valor adquisición", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(23), "Valor actual", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(24), "Respecto a mes anterior", IndexedColors.LIME, false);

        //Pasivos
        createCellWithFormat(workbook, header.createCell(26), "Concepto", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(27), "Tipo", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(28), "Cantidad inicial", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(29), "Tasa de interes", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(30), "Fecha inicio", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(31), "Fecha fin", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(32), "Saldo pendiente", IndexedColors.YELLOW1, false);
        createCellWithFormat(workbook, header.createCell(33), "Respecto a mes anterior", IndexedColors.YELLOW1, false);
    }

    private static void writeHeaderRow4(Sheet sheet, Workbook workbook) {
        Row header = sheet.getRow(3);

        //Activos
        createCellWithFormat(workbook, header.createCell(19), "Total", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(20), "", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(21), "", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(22), "", IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(23), sumaRango("Y5","Y52"), IndexedColors.LIME, false);
        createCellWithFormat(workbook, header.createCell(24), sumaRango("Z5","Z52"), IndexedColors.LIME, false);

        //Pasivos
        createCellWithFormat(workbook, header.createCell(26), "Total", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(27), "", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(28), "", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(29), "", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(30), "", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(31), "", IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(32), sumaRango("AG5", "AG52"), IndexedColors.GOLD, false);
        createCellWithFormat(workbook, header.createCell(33), sumaRango("AH5", "AH52"), IndexedColors.GOLD, false);
    }

    private void aplicarFormatosMonedaYFecha(Sheet sheet, Workbook workbook) {
        DataFormat format = workbook.createDataFormat();

        // Formatos
        short monedaFormat = format.getFormat("#,##0.00 €");
        short fechaFormat = format.getFormat("dd/mm/yyyy");

        // Columnas de moneda (índices base 0)
        int[] columnasMoneda = {2, 9, 16}; // C, J, Q
        for (int col : columnasMoneda) {
            for (int row = 0; row <= sheet.getLastRowNum(); row++) {
                Row r = sheet.getRow(row);
                if (r != null) {
                    Cell c = r.getCell(col);
                    if (c != null) {
                        CellStyle style = workbook.createCellStyle();
                        style.cloneStyleFrom(c.getCellStyle());
                        style.setDataFormat(monedaFormat);
                        c.setCellStyle(style);
                    }
                }
            }
        }
        // Columnas de fecha en transacciones (10 y 17)
        int[] colsFechaTrans = {10, 17};
        for (int col : colsFechaTrans) {
            for (int row = 2; row <= 51; row++) {
                Row r = sheet.getRow(row);
                if (r != null) {
                    Cell c = r.getCell(col);
                    if (c != null) {
                        CellStyle style = workbook.createCellStyle();
                        style.cloneStyleFrom(c.getCellStyle());
                        style.setDataFormat(fechaFormat);
                        c.setCellStyle(style);
                    }
                }
            }
        }
        // W (col 22) filas 5-52
        for (int row = 4; row <= 51; row++) {
            Row r = sheet.getRow(row);
            if (r != null) {
                Cell c = r.getCell(22);
                if (c != null) {
                    CellStyle style = workbook.createCellStyle();
                    style.cloneStyleFrom(c.getCellStyle());
                    style.setDataFormat(monedaFormat);
                    c.setCellStyle(style);
                }
            }
        }
        // X (col 23), Y (col 24), AG (col 32), AH (col 33) filas 4-52
        int[] colsMonedaRango = {23, 24, 32, 33};
        for (int col : colsMonedaRango) {
            for (int row = 3; row <= 51; row++) {
                Row r = sheet.getRow(row);
                if (r != null) {
                    Cell c = r.getCell(col);
                    if (c != null) {
                        CellStyle style = workbook.createCellStyle();
                        style.cloneStyleFrom(c.getCellStyle());
                        style.setDataFormat(monedaFormat);
                        c.setCellStyle(style);
                    }
                }
            }
        }
        // AC (col 28) filas 5-52
        for (int row = 4; row <= 51; row++) {
            Row r = sheet.getRow(row);
            if (r != null) {
                Cell c = r.getCell(28);
                if (c != null) {
                    CellStyle style = workbook.createCellStyle();
                    style.cloneStyleFrom(c.getCellStyle());
                    style.setDataFormat(monedaFormat);
                    c.setCellStyle(style);
                }
            }
        }
        // U (col 20), AE (col 30), AF (col 31) filas 5-52
        int[] colsFecha = {20, 30, 31};
        for (int col : colsFecha) {
            for (int row = 4; row <= 51; row++) {
                Row r = sheet.getRow(row);
                if (r != null) {
                    Cell c = r.getCell(col);
                    if (c != null) {
                        CellStyle style = workbook.createCellStyle();
                        style.cloneStyleFrom(c.getCellStyle());
                        style.setDataFormat(fechaFormat);
                        c.setCellStyle(style);
                    }
                }
            }
        }
    }
    private static void createCellWithFormat(Workbook workbook, Cell cell, String value, IndexedColors color, boolean alignCenter) {
        boolean isFormula = value != null && value.startsWith("=");

        if (isFormula && value != null) {
            // quitar el '=' inicial para setCellFormula
            try {
                cell.setCellFormula(value.substring(1));
            } catch (Exception e) {
                // fallback a texto si la fórmula no es válida para POI
                cell.setCellValue(value);
            }
        } else {
            cell.setCellValue(value == null ? "" : value);
        }

        //fondo
        CellStyle bgStyle = workbook.createCellStyle();
        bgStyle.setFillForegroundColor(color.getIndex());
        bgStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        //negrita
        Font font = workbook.createFont();
        font.setBold(true);
        bgStyle.setFont(font);

        //borde
        bgStyle.setBorderTop(BorderStyle.THIN);
        bgStyle.setBorderBottom(BorderStyle.THIN);
        bgStyle.setBorderLeft(BorderStyle.THIN);
        bgStyle.setBorderRight(BorderStyle.THIN);

        if (alignCenter) {
            bgStyle.setAlignment(HorizontalAlignment.CENTER);
            bgStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        cell.setCellStyle(bgStyle);
    }

    private static String sumaRango(String cellA, String cellB) {
        if (cellA == null || cellB == null) return "";
        cellA = cellA.trim().toUpperCase();
        cellB = cellB.trim().toUpperCase();
        try {
            CellReference refA = new CellReference(cellA);
            CellReference refB = new CellReference(cellB);
            int colA = refA.getCol();
            int colB = refB.getCol();
            int rowA = refA.getRow();
            int rowB = refB.getRow();

            if (colA == colB) {
                int r1 = Math.min(rowA, rowB);
                int r2 = Math.max(rowA, rowB);
                String start = CellReference.convertNumToColString(colA) + (r1 + 1);
                String end = CellReference.convertNumToColString(colA) + (r2 + 1);
                return "=SUM(" + start + ":" + end + ")";
            } else if (rowA == rowB) {
                int c1 = Math.min(colA, colB);
                int c2 = Math.max(colA, colB);
                String start = CellReference.convertNumToColString(c1) + (rowA + 1);
                String end = CellReference.convertNumToColString(c2) + (rowA + 1);
                return "=SUM(" + start + ":" + end + ")";
            } else {
                // Diferentes fila y columna: no tiene sentido un rango rectángulo para SUM simple -> usar SUM(a,b)
                return "=SUM(" + cellA + "," + cellB + ")";
            }
        } catch (Exception e) {
            return "=SUM(" + cellA + "," + cellB + ")";
        }
    }

    private static void aplicarFormulas(XSSFWorkbook workbook) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll(); // intenta evaluar y cachear resultados
        workbook.setForceFormulaRecalculation(true);
    }
    private Double getPreviousAssetValue(Long assetId, LocalDate date) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT current_value FROM asset_values WHERE asset_id = ? AND valuation_date <= ? ORDER BY valuation_date DESC LIMIT 1",
                    Double.class, assetId, date);
        } catch (DataAccessException e) {
            return null;
        }
    }
    private Double getPreviousLiabilityOutstanding(Long liabilityId, LocalDate date) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT outstanding_balance FROM liability_values WHERE liability_id = ? AND valuation_date = ?",
                    Double.class, liabilityId, date);
        } catch (DataAccessException e) {
            return null;
        }
    }
    private Double sumAssetTypeForMonth(Long userId, String assetTypeName, LocalDate valuationDate) {
        // Obtener la última valoración de cada activo hasta la fecha especificada
        String sql = "SELECT COALESCE(SUM(latest_values.current_value), 0) " +
                "FROM ( " +
                "    SELECT DISTINCT ON (a.asset_id) a.asset_id, av.current_value " +
                "    FROM assets a " +
                "    JOIN asset_types at ON a.asset_type_id = at.asset_type_id " +
                "    JOIN asset_values av ON av.asset_id = a.asset_id AND av.valuation_date <= ? " +
                "    WHERE a.user_id = ? AND at.name = ? " +
                "    ORDER BY a.asset_id, av.valuation_date DESC " +
                ") AS latest_values";
        try {
            Double result = jdbcTemplate.queryForObject(sql, Double.class, valuationDate, userId, assetTypeName);
            return result != null ? result : 0.0;
        } catch (DataAccessException e) {
            return 0.0;
        }
    }

    private double sumCuentaBancariaCurrentMonth(Long userId, LocalDate valuationDate) {
        return sumAssetTypeForMonth(userId, "Cuenta bancaria", valuationDate);
    }

    private double sumFondoInversionCurrentMonth(Long userId, LocalDate valuationDate) {
        return sumAssetTypeForMonth(userId, "Fondo de inversión", valuationDate);
    }
}
