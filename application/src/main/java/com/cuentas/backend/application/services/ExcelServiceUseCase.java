package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ExcelServicePort;
import com.cuentas.backend.application.ports.driving.FileServicePort;
import com.cuentas.backend.domain.File;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ExcelServiceUseCase implements ExcelServicePort {

    private final JdbcTemplate jdbcTemplate;
    private final FileServicePort fileServicePort;

    public ExcelServiceUseCase(JdbcTemplate jdbcTemplate, FileServicePort fileServicePort) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileServicePort = fileServicePort;
    }

    @Override
    @Transactional
    public void processExcel(File excelFile, int year, long userId) {
        fileServicePort.saveFile(excelFile);

        var data = excelFile.getFileData();
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("File data vacío");
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
                LocalDate date = LocalDate.of(year, month, 1);

                // celdas de control
                double[] resultsArray = new double[]{
                        getCellNumericValue(sheet, "C2", formatter, evaluator),
                        getCellNumericValue(sheet, "C3", formatter, evaluator),
                        getCellNumericValue(sheet, "C4", formatter, evaluator),
                        getCellNumericValue(sheet, "C6", formatter, evaluator),
                        getCellNumericValue(sheet, "C7", formatter, evaluator),
                        getCellNumericValue(sheet, "C8", formatter, evaluator)
                };

                // matrices
                List<List<Object>> incomeMatrix = readMatrix(sheet, "F", "G", 3, formatter, evaluator);
                List<List<Object>> expenseMatrix = readMatrix(sheet, "I", "J", 3, formatter, evaluator);
                List<List<Object>> liquidMatrix = readMatrix(sheet, "L", "M", 5, formatter, evaluator);
                List<List<Object>> investedMatrix = readMatrix(sheet, "P", "Q", 5, formatter, evaluator);
                List<List<Object>> investmentsMatrix = readMatrix4Cols(sheet, "T", "U", "V", "W", 5, formatter, evaluator);

                // borrar resultados antiguos
                jdbcTemplate.update("DELETE FROM results WHERE UserID = ? AND year = ? AND month = ?", userId, year, month);
                createResults(resultsArray, month, year, userId, date);

                // borrar registros antiguos
                jdbcTemplate.update("DELETE FROM records WHERE UserID = ? AND year = ? AND month = ?", userId, year, month);

                // crear registros
                createRecords(incomeMatrix, 1, month, year, userId, date, false);
                createRecords(expenseMatrix, 2, month, year, userId, date, false);
                createRecords(liquidMatrix, 3, month, year, userId, date, false);
                createRecords(investedMatrix, 4, month, year, userId, date, false);
                createRecords(investmentsMatrix, null, month, year, userId, date, true);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error procesando Excel", e);
        }
    }

    // ===== helpers de lectura =====

    private double getCellNumericValue(Sheet sheet, String cellRef, DataFormatter formatter, FormulaEvaluator evaluator) {
        CellReference ref = new CellReference(cellRef);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) return 0;
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) return 0;
        String txt = formatter.formatCellValue(cell, evaluator);
        return parseDoubleSafe(txt);
    }

    private List<List<Object>> readMatrix(Sheet sheet, String colCategory, String colAmount, int startRow,
                                          DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<Object>> matrix = new ArrayList<>();
        int start = Math.max(0, startRow - 1);
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) break;
            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));
            if ((catCell == null || formatter.formatCellValue(catCell, evaluator).isBlank())
                    && (amountCell == null || formatter.formatCellValue(amountCell, evaluator).isBlank())) {
                if (rowNum > start + 0) break;
            }
            String category = catCell != null ? formatter.formatCellValue(catCell, evaluator) : "Sin categoría";
            double amount = amountCell != null ? parseDoubleSafe(formatter.formatCellValue(amountCell, evaluator)) : 0;
            matrix.add(new ArrayList<>(List.of(category, amount)));
        }
        return matrix;
    }

    private List<List<Object>> readMatrix4Cols(Sheet sheet, String col1, String col2, String col3, String col4, int startRow,
                                               DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<Object>> matrix = new ArrayList<>();
        int start = Math.max(0, startRow - 1);
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) break;
            Cell cell1 = row.getCell(CellReference.convertColStringToIndex(col1));
            Cell cell2 = row.getCell(CellReference.convertColStringToIndex(col2));
            Cell cell3 = row.getCell(CellReference.convertColStringToIndex(col3));
            Cell cell4 = row.getCell(CellReference.convertColStringToIndex(col4));
            boolean allEmpty =
                    (cell1 == null || formatter.formatCellValue(cell1, evaluator).isBlank()) &&
                            (cell2 == null || formatter.formatCellValue(cell2, evaluator).isBlank()) &&
                            (cell3 == null || formatter.formatCellValue(cell3, evaluator).isBlank()) &&
                            (cell4 == null || formatter.formatCellValue(cell4, evaluator).isBlank());
            if (allEmpty) break;

            String category = cell1 != null ? formatter.formatCellValue(cell1, evaluator) : "Sin categoría";
            double cost = cell2 != null ? parseDoubleSafe(formatter.formatCellValue(cell2, evaluator)) : 0;
            double income = cell3 != null ? parseDoubleSafe(formatter.formatCellValue(cell3, evaluator)) : 0;
            double profit = cell4 != null ? parseDoubleSafe(formatter.formatCellValue(cell4, evaluator)) : 0;
            matrix.add(new ArrayList<>(List.of(category, cost, income, profit)));
        }
        return matrix;
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.isBlank()) return 0;
        val = val.replace(",", ".");
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ===== creación de resultados y registros =====

    private void createResults(double[] resultsArray, int month, int year, long userId, LocalDate date) {
        for (int i = 0; i < resultsArray.length; i++) {
            jdbcTemplate.update(
                    "INSERT INTO results (UserID, type, amount, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?)",
                    userId, i + 1, resultsArray[i], year, month, date
            );
        }
    }

    private void createRecords(List<List<Object>> matrix, Integer type, int month, int year, long userId, LocalDate date, boolean isInvestment) {
        // primero mapear categorías a IDs
        mapCategories(matrix, userId);

        if (!isInvestment) {
            for (List<Object> row : matrix) {
                Long categoryId = (Long) row.get(0);  // ✅ ahora es ID
                double amount = toDouble(row.get(1));
                jdbcTemplate.update(
                        "INSERT INTO records (UserID, type, category, amount, profit, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        userId, type, categoryId, amount, 0, year, month, date
                );
            }
        } else {
            for (List<Object> row : matrix) {
                Long categoryId = (Long) row.get(0);  // ✅ ahora es ID
                double cost = toDouble(row.get(1));
                double income = toDouble(row.get(2));
                double profit = toDouble(row.get(3));

                if (cost == 0 && income > 0) {
                    jdbcTemplate.update(
                            "INSERT INTO records (UserID, type, category, amount, profit, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            userId, 6, categoryId, income, profit, year, month, date
                    );
                } else if (cost > 0 && income == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO records (UserID, type, category, amount, profit, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            userId, 5, categoryId, cost, 0, year, month, date
                    );
                } else if (cost > 0 && income > 0) {
                    // compra
                    jdbcTemplate.update(
                            "INSERT INTO records (UserID, type, category, amount, profit, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            userId, 5, categoryId, cost, 0, year, month, date
                    );
                    // venta
                    jdbcTemplate.update(
                            "INSERT INTO records (UserID, type, category, amount, profit, year, month, r_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            userId, 6, categoryId, income, profit, year, month, date
                    );
                }
            }
        }
    }

    private void mapCategories(List<List<Object>> matrix, long userId) {
        for (List<Object> row : matrix) {
            String category = (String) row.get(0);
            if (category == null || category.isBlank()) {
                category = "Sin categoría";
            }

            final String categoryFinal = category;

            try {
                // ✅ Recuperar el ID si existe
                Long existingId = jdbcTemplate.queryForObject(
                        "SELECT ID FROM categories WHERE UserID = ? AND category_name = ?",
                        Long.class, userId, categoryFinal
                );
                row.set(0, existingId);  // ✅ Guardar ID en vez del nombre
            } catch (Exception e) {
                // ✅ Insertar si no existe y recuperar el ID
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO categories (UserID, category_name) VALUES (?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setLong(1, userId);
                    ps.setString(2, categoryFinal);
                    return ps;
                }, keyHolder);

                // ✅ Extraer solo la columna "id"
                Number newId = (Number) keyHolder.getKeys().get("id");
                row.set(0, newId.longValue());
            }
        }
    }


    private int getMonthNumber(String monthName) {
        switch (monthName) {
            case "Enero": return 1;
            case "Febrero": return 2;
            case "Marzo": return 3;
            case "Abril": return 4;
            case "Mayo": return 5;
            case "Junio": return 6;
            case "Julio": return 7;
            case "Agosto": return 8;
            case "Septiembre": return 9;
            case "Octubre": return 10;
            case "Noviembre": return 11;
            case "Diciembre": return 12;
            default: return 0;
        }
    }

    private double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
