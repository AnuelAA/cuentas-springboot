// language: java
package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ExcelNewServicePort;
import com.cuentas.backend.domain.File;
import com.cuentas.backend.domain.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ExcelNewServiceUseCase implements ExcelNewServicePort {

    private static final Logger log = LoggerFactory.getLogger(ExcelNewServiceUseCase.class);
    private final JdbcTemplate jdbcTemplate;

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
                int assetCount = saveAssets(sheet, userId, defaultDate, "R", "S","T","U","V");
                int liabilityCount = saveLiabilities(sheet, userId, defaultDate, "Y", "Z", "AA","AB","AC","AD","AE");

                // luego ingresos y gastos
                List<Transaction> incomeTransactionList = saveIncome(sheet, userId,"F", "G","H","I","J", defaultDate);
                List<Transaction> expenseTransactionList = saveExpense(sheet, userId,"L","M","N","O", "P", defaultDate);

                log.debug("Filas leídas - Ingresos: {}, Gastos: {}, Activos: {}, Pasivos: {}",
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
            LocalDate acqDate = acqDateCell != null && !acqDateCell.toString().isBlank() ? LocalDate.parse(acqDateCell.toString(), java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : null;
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
            LocalDate startDate = startDateCell != null && !startDateCell.toString().isBlank() ? LocalDate.parse(startDateCell.toString(), java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : null;
            LocalDate endDate = endDateCell != null && !endDateCell.toString().isBlank() ? LocalDate.parse(endDateCell.toString(), java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : null;
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
                                         String colLiability, String colRelatedAsset, String colAmount, LocalDate defaultDate) {
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

            if (catCell == null || catCell.toString().isBlank() ||
                    amountCell == null || amountCell.toString().isBlank()) continue;

            Long categoryId = getCategoryId(userId,catCell.toString());
            Long assetId = assetCell != null && !assetCell.toString().isBlank() ? getAssetId(userId,assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && !liabilityCell.toString().isBlank() ? getLiabilityId(userId,liabilityCell.toString()) : null;
            Long relatedAssetId = relatedAssetCell != null && !relatedAssetCell.toString().isBlank() ? getAssetId(userId,relatedAssetCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, relatedAssetId, "income", amount, defaultDate);

            transactionList.add(new Transaction(userId,categoryId,assetId,liabilityId,relatedAssetId,amount,"income",defaultDate));
        }
        return transactionList;
    }


    private List<Transaction> saveExpense(Sheet sheet, Long userId, String colCategory, String colAsset,
                                          String colLiability, String colRelatedAsset, String colAmount, LocalDate defaultDate) {
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

            if (catCell == null || catCell.toString().isBlank() ||
                    amountCell == null || amountCell.toString().isBlank()) continue;

            Long categoryId = getCategoryId(userId,catCell.toString());
            Long assetId = assetCell != null && !assetCell.toString().isBlank() ? getAssetId(userId,assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && !liabilityCell.toString().isBlank() ? getLiabilityId(userId,liabilityCell.toString()) : null;
            Long relatedAssetId = relatedAssetCell != null && !relatedAssetCell.toString().isBlank() ? getAssetId(userId,relatedAssetCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, relatedAssetId, "expense", amount, defaultDate);

            transactionList.add(new Transaction(userId,categoryId,assetId,liabilityId,relatedAssetId,amount,"expense",defaultDate));
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
        return jdbcTemplate.queryForObject(SQL_SELECT_ASSET, Long.class,userId, name);
    }

    private Long getLiabilityTypeId(String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY_TYPE_ID, Long.class, name);
    }

    private Long getLiabilityId(Long userId, String name) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY, Long.class,userId, name);
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
}
