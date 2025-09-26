package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ExcelServicePort;
import com.cuentas.backend.domain.File;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ExcelServiceUseCase implements ExcelServicePort {

    private static final Logger log = LoggerFactory.getLogger(ExcelServiceUseCase.class);
    private final JdbcTemplate jdbcTemplate;

    // =======================
    // Constantes SQL
    // =======================
    private static final String SQL_INSERT_CATEGORY =
            "INSERT INTO categories (user_id, name, type, created_at) VALUES (?, ?, ?, NOW()) RETURNING category_id";
    private static final String SQL_SELECT_CATEGORY =
            "SELECT category_id FROM categories WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_TRANSACTION =
            "INSERT INTO transactions (user_id, category_id, asset_id, liability_id, amount, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ASSET =
            "SELECT asset_id FROM assets WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_ASSET =
            "INSERT INTO assets (user_id, asset_type_id, name, acquisition_value, current_value, acquisition_date, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW()) RETURNING asset_id";
    private static final String SQL_UPDATE_ASSET =
            "UPDATE assets SET current_value = ?, updated_at = NOW() WHERE asset_id = ?";
    private static final String SQL_SELECT_LIABILITY =
            "SELECT liability_id FROM liabilities WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_LIABILITY =
            "INSERT INTO liabilities (user_id, liability_type_id, name, principal_amount, outstanding_balance, start_date, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW()) RETURNING liability_id";
    private static final String SQL_UPDATE_LIABILITY =
            "UPDATE liabilities SET outstanding_balance = ?, updated_at = NOW() WHERE liability_id = ?";

    public ExcelServiceUseCase(JdbcTemplate jdbcTemplate) {
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

            List<String> months = Arrays.asList(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            );

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (String monthName : months) {
                Sheet sheet = workbook.getSheet(monthName);
                if (sheet == null) continue;

                int month = getMonthNumber(monthName);
                LocalDate defaultDate = LocalDate.of(year, month, 1);

                log.info("Procesando hoja [{}] para usuario {} en año {}", monthName, userId, year);

                List<List<Object>> incomeMatrix = readMatrix(sheet, "F", "G", 3, formatter, evaluator);
                List<List<Object>> expenseMatrix = readMatrix(sheet, "I", "J", 3, formatter, evaluator);
                List<List<Object>> liquidMatrix = readMatrix(sheet, "L", "M", 5, formatter, evaluator);
                List<List<Object>> investedMatrix = readMatrix(sheet, "P", "Q", 5, formatter, evaluator);
                List<List<Object>> investmentsMatrix = readMatrix4Cols(sheet, "T", "U", "V", "W", 5, formatter, evaluator);

                log.debug("Matrices leídas - Ingresos: {}, Gastos: {}, Liquidez: {}, Pasivos: {}, Activos: {}",
                        incomeMatrix.size(), expenseMatrix.size(), liquidMatrix.size(),
                        investedMatrix.size(), investmentsMatrix.size());

                saveTransactions(incomeMatrix, userId, "income", defaultDate);
                saveTransactions(expenseMatrix, userId, "expense", defaultDate);
                saveTransactions(liquidMatrix, userId, "income", defaultDate);

                saveAssets(investmentsMatrix, userId, defaultDate);
                saveLiabilities(investedMatrix, userId, defaultDate);
            }

        } catch (Exception e) {
            log.error("Error procesando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando Excel", e);
        }
    }

    // =======================
    // Guardado de entidades
    // =======================
    private void saveTransactions(List<List<Object>> matrix, long userId, String type, LocalDate defaultDate) {
        mapCategories(matrix, userId, type);
        for (List<Object> row : matrix) {
            Long categoryId = (Long) row.get(0);
            double amount = toDouble(row.get(1));
            LocalDate transactionDate = row.size() > 2 && row.get(2) instanceof LocalDate
                    ? (LocalDate) row.get(2)
                    : defaultDate;

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, null, null, amount, transactionDate);
            log.trace("Insertada transacción [{} - {}€] en fecha {}", categoryId, amount, transactionDate);
        }
    }

    private void saveAssets(List<List<Object>> matrix, long userId, LocalDate defaultDate) {
        for (List<Object> row : matrix) {
            String name = (String) row.get(0);
            double acquisitionValue = toDouble(row.get(1));
            double currentValue = row.size() > 2 ? toDouble(row.get(2)) : acquisitionValue;
            LocalDate acquisitionDate = row.size() > 3 && row.get(3) instanceof LocalDate
                    ? (LocalDate) row.get(3)
                    : defaultDate;

            Long assetTypeId = getAssetTypeId("Fondo de inversión"); // por defecto
            Long assetId = upsertAsset(userId, name, acquisitionValue, currentValue, acquisitionDate, assetTypeId);

            if (row.size() > 2) {
                double income = toDouble(row.get(2));
                if (income > 0) {
                    jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, null, assetId, null, income, acquisitionDate);
                    log.trace("Registrado ingreso de activo [{}] = {}€", assetId, income);
                }
            }
        }
    }

    private void saveLiabilities(List<List<Object>> matrix, long userId, LocalDate defaultDate) {
        for (List<Object> row : matrix) {
            String name = (String) row.get(0);
            double principal = toDouble(row.get(1));
            double outstanding = row.size() > 2 ? toDouble(row.get(2)) : principal;
            LocalDate startDate = row.size() > 3 && row.get(3) instanceof LocalDate
                    ? (LocalDate) row.get(3)
                    : defaultDate;

            Long liabilityTypeId = getLiabilityTypeId("Hipoteca");
            Long liabilityId = upsertLiability(userId, name, principal, outstanding, startDate, liabilityTypeId);

            if (row.size() > 2) {
                double payment = toDouble(row.get(2));
                if (payment > 0) {
                    jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, null, null, liabilityId, payment, startDate);
                    log.trace("Registrado pago de pasivo [{}] = {}€", liabilityId, payment);
                }
            }
        }
    }

    // =======================
    // Helpers de mapeo
    // =======================
    private void mapCategories(List<List<Object>> matrix, long userId, String type) {
        if (matrix == null) return;
        for (List<Object> row : matrix) {
            String category = (String) row.get(0);
            if (category == null || category.isBlank()) category = "Sin categoría";

            try {
                Long existingId = jdbcTemplate.queryForObject(SQL_SELECT_CATEGORY, Long.class, userId, category);
                row.set(0, existingId);
                log.trace("Categoria existente: '{}' -> id={}", category, existingId);
            } catch (DataAccessException e) {
                String catFinal = (category == null || category.isBlank()) ? "Sin categoría" : category;
                Long categoryId = insertAndReturnId(SQL_INSERT_CATEGORY, ps -> {
                    ps.setLong(1, userId);
                    ps.setString(2, catFinal);
                    ps.setString(3, type);
                });
                row.set(0, categoryId);
                log.debug("Insertada nueva categoría [{}] para usuario {} con id={}", category, userId, categoryId);
            }
        }
    }

    private Long upsertAsset(long userId, String name, double acquisitionValue,
                             double currentValue, LocalDate acquisitionDate, Long assetTypeId) {
        try {
            Long assetId = jdbcTemplate.queryForObject(SQL_SELECT_ASSET, Long.class, userId, name);
            jdbcTemplate.update(SQL_UPDATE_ASSET, currentValue, assetId);
            return assetId;
        } catch (DataAccessException e) {
            return insertAndReturnId(SQL_INSERT_ASSET, ps -> {
                ps.setLong(1, userId);
                ps.setLong(2, assetTypeId);
                ps.setString(3, name);
                ps.setDouble(4, acquisitionValue);
                ps.setDouble(5, currentValue);
                ps.setDate(6, java.sql.Date.valueOf(acquisitionDate));
            });
        }
    }

    private Long upsertLiability(long userId, String name, double principal, double outstanding,
                                 LocalDate startDate, Long liabilityTypeId) {
        try {
            Long liabilityId = jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY, Long.class, userId, name);
            jdbcTemplate.update(SQL_UPDATE_LIABILITY, outstanding, liabilityId);
            return liabilityId;
        } catch (DataAccessException e) {
            return insertAndReturnId(SQL_INSERT_LIABILITY, ps -> {
                ps.setLong(1, userId);
                ps.setLong(2, liabilityTypeId);
                ps.setString(3, name);
                ps.setDouble(4, principal);
                ps.setDouble(5, outstanding);
                ps.setDate(6, java.sql.Date.valueOf(startDate));
            });
        }
    }

    // =======================
    // Inserciones homogéneas (Postgres RETURNING)
    // =======================
    private Long insertAndReturnId(String sql, ThrowingConsumer<PreparedStatement> consumer) {
        return jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            try {
                consumer.accept(ps); // capturamos la Exception
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return ps;
        }, (ResultSet rs) -> {
            if (rs.next()) return rs.getLong(1);
            throw new IllegalStateException("INSERT no devolvió id (RETURNING)");
        });
    }


    private Long getAssetTypeId(String name) {
        return jdbcTemplate.queryForObject("SELECT asset_type_id FROM asset_types WHERE name = ?", Long.class, name);
    }

    private Long getLiabilityTypeId(String name) {
        return jdbcTemplate.queryForObject("SELECT liability_type_id FROM liability_types WHERE name = ?", Long.class, name);
    }

    // =======================
    // Lectura Excel
    // =======================
    private List<List<Object>> readMatrix(Sheet sheet, String colCategory, String colAmount, int startRow,
                                          DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<Object>> matrix = new ArrayList<>();
        int start = Math.max(0, startRow - 1);
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) break;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));

            if ((catCell == null || formatter.formatCellValue(catCell, evaluator).isBlank()) &&
                    (amountCell == null || formatter.formatCellValue(amountCell, evaluator).isBlank())) {
                if (rowNum > start) break;
            }

            String category = catCell != null ? formatter.formatCellValue(catCell, evaluator) : "Sin categoría";
            double amount = amountCell != null ? parseDoubleSafe(formatter.formatCellValue(amountCell, evaluator)) : 0;
            matrix.add(new ArrayList<>(List.of(category, amount)));
        }
        return matrix;
    }

    private List<List<Object>> readMatrix4Cols(Sheet sheet, String col1, String col2, String col3, String col4,
                                               int startRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<Object>> matrix = new ArrayList<>();
        int start = Math.max(0, startRow - 1);
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) break;

            Cell c1 = row.getCell(CellReference.convertColStringToIndex(col1));
            Cell c2 = row.getCell(CellReference.convertColStringToIndex(col2));
            Cell c3 = row.getCell(CellReference.convertColStringToIndex(col3));
            Cell c4 = row.getCell(CellReference.convertColStringToIndex(col4));

            boolean allEmpty =
                    (c1 == null || formatter.formatCellValue(c1, evaluator).isBlank()) &&
                            (c2 == null || formatter.formatCellValue(c2, evaluator).isBlank()) &&
                            (c3 == null || formatter.formatCellValue(c3, evaluator).isBlank()) &&
                            (c4 == null || formatter.formatCellValue(c4, evaluator).isBlank());
            if (allEmpty) break;

            String category = c1 != null ? formatter.formatCellValue(c1, evaluator) : "Sin categoría";
            double v2 = c2 != null ? parseDoubleSafe(formatter.formatCellValue(c2, evaluator)) : 0;
            double v3 = c3 != null ? parseDoubleSafe(formatter.formatCellValue(c3, evaluator)) : 0;
            double v4 = c4 != null ? parseDoubleSafe(formatter.formatCellValue(c4, evaluator)) : 0;
            matrix.add(new ArrayList<>(List.of(category, v2, v3, v4)));
        }
        return matrix;
    }

    // =======================
    // Utilidades
    // =======================
    private double parseDoubleSafe(String val) {
        if (val == null || val.isBlank()) return 0;
        val = val.replace(",", ".");
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return 0; }
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

    private double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
