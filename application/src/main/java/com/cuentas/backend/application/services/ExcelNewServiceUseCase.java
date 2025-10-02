package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.ExcelNewServicePort;
import com.cuentas.backend.domain.Asset;
import com.cuentas.backend.domain.File;
import com.cuentas.backend.domain.Liability;
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
    private static final String SQL_INSERT_CATEGORY =
            "INSERT INTO categories (user_id, name, created_at) VALUES (?, ?, NOW()) RETURNING category_id";
    private static final String SQL_SELECT_CATEGORY =
            "SELECT category_id FROM categories WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_TRANSACTION =
            "INSERT INTO transactions (user_id, category_id, asset_id, liability_id, transaction_type, amount, transaction_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ASSET =
            "SELECT asset_id FROM assets WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_ASSET =
            "INSERT INTO assets (user_id, asset_type_id, name, current_value, created_at) VALUES (?, ?, ?, ?, NOW()) RETURNING asset_id";
    private static final String SQL_UPDATE_ASSET =
            "UPDATE assets SET current_value = ?, updated_at = NOW() WHERE asset_id = ?";
    private static final String SQL_SELECT_LIABILITY =
            "SELECT liability_id FROM liabilities WHERE user_id = ? AND name = ?";
    private static final String SQL_INSERT_LIABILITY =
            "INSERT INTO liabilities (user_id, liability_type_id, name, outstanding_balance, created_at) VALUES (?, ?, ?, ?, NOW()) RETURNING liability_id";
    private static final String SQL_UPDATE_LIABILITY =
            "UPDATE liabilities SET outstanding_balance = ?, updated_at = NOW() WHERE liability_id = ?";

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

                //primero leemos y guardamos los activos y pasivos
                List<Asset> assetList = saveAssets(sheet, userId,"P", "Q","R");
                List<Liability> liabilityList = saveLiabilities(sheet,userId, "U", "V", "W");

                //luego ingresos y gastos
                List<Transaction> incomeTransactionList = saveIncome(sheet, userId,"F", "G","H","I", defaultDate);
                List<Transaction> expenseTransactionList = saveExpense(sheet, userId,"K", "L","M","N", defaultDate);

                log.debug("Filas leídas - Ingresos: {}, Gastos: {}, Activos: {}, Pasivos: {}",
                        incomeTransactionList.size(), expenseTransactionList.size(), assetList.size(), liabilityList.size());

            }

        } catch (Exception e) {
            log.error("Error procesando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando Excel", e);
        }
    }

    // =======================
    // Lectura Excel y guardado en BD
    // =======================
    private List<Asset> saveAssets(Sheet sheet, Long userId, String colCategory, String colType, String colCurrentValue) {
        List<Asset> assetList = new ArrayList<>();
        int start = 4; // fila 5 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell typeCell = row.getCell(CellReference.convertColStringToIndex(colType));
            Cell currentValueCell = row.getCell(CellReference.convertColStringToIndex(colCurrentValue));

            if (catCell == null || catCell.toString() == "" ||
                    currentValueCell == null || currentValueCell.toString() == ""
                    || typeCell == null || typeCell.toString() == "") continue;

            String name = catCell.toString() ;
            Long assetTypeId = getAssetTypeId(typeCell.toString());
            var currentValue = evaluaSumaResta(currentValueCell.toString());

            upsertAsset(userId, assetTypeId, name, currentValue);

            assetList.add(new Asset(name,userId,assetTypeId,currentValue));
        }
        return assetList;
    }
    private List<Liability> saveLiabilities(Sheet sheet, Long userId, String colCategory, String colType, String colOutstandingBalance) {
        List<Liability> liabilityList = new ArrayList<>();
        int start = 4; // fila 5 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell typeCell = row.getCell(CellReference.convertColStringToIndex(colType));
            Cell outstandingBalanceCell = row.getCell(CellReference.convertColStringToIndex(colOutstandingBalance));

            if (catCell == null || catCell.toString() == "" ||
                    outstandingBalanceCell == null || outstandingBalanceCell.toString() == ""
                    || typeCell == null || typeCell.toString() == "") continue;

            String name = catCell.toString();
            Long liabilityTypeId = getLiabilityTypeId(typeCell.toString());
            var outstandingBalance = evaluaSumaResta(outstandingBalanceCell.toString());

            upsertLiability(userId, liabilityTypeId, name, outstandingBalance);

            liabilityList.add(new Liability(name,userId,liabilityTypeId,outstandingBalance));
        }
        return liabilityList;
    }

    private List<Transaction> saveIncome(Sheet sheet, Long userId, String colCategory, String colAsset, String colLiability, String colAmount, LocalDate defaultDate) {
        List<Transaction> transactionList = new ArrayList<>();
        int start = 2; // fila 3 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell assetCell = row.getCell(CellReference.convertColStringToIndex(colAsset));
            Cell liabilityCell = row.getCell(CellReference.convertColStringToIndex(colLiability));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));

            if (catCell == null || catCell.toString() == "" ||
                    amountCell == null || amountCell.toString() == "") continue;

            Long categoryId = getCategoryId(userId,catCell.toString());
            Long assetId = assetCell != null && assetCell.toString() != "" ? getAssetId(userId,assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && liabilityCell.toString() != "" ? getLiabilityId(userId,liabilityCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, "income", amount, defaultDate);

            transactionList.add(new Transaction(userId,categoryId,assetId,liabilityId,amount,defaultDate));
        }
        return transactionList;
    }


    private List<Transaction> saveExpense(Sheet sheet, Long userId, String colCategory, String colAsset, String colLiability, String colAmount, LocalDate defaultDate) {
        List<Transaction> transactionList = new ArrayList<>();
        int start = 2; // fila 3 en Excel
        for (int rowNum = start; rowNum < 102; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell catCell = row.getCell(CellReference.convertColStringToIndex(colCategory));
            Cell assetCell = row.getCell(CellReference.convertColStringToIndex(colAsset));
            Cell liabilityCell = row.getCell(CellReference.convertColStringToIndex(colLiability));
            Cell amountCell = row.getCell(CellReference.convertColStringToIndex(colAmount));

            if (catCell == null || catCell.toString() == "" ||
                    amountCell == null || amountCell.toString() == "") continue;

            Long categoryId = getCategoryId(userId,catCell.toString());
            Long assetId = assetCell != null && assetCell.toString() != "" ? getAssetId(userId,assetCell.toString()) : null;
            Long liabilityId = liabilityCell != null && liabilityCell.toString() != "" ? getLiabilityId(userId,liabilityCell.toString()) : null;
            double amount = evaluaSumaResta(amountCell.toString());

            jdbcTemplate.update(SQL_INSERT_TRANSACTION, userId, categoryId, assetId, liabilityId, "expense", amount, defaultDate);

            transactionList.add(new Transaction(userId,categoryId,assetId,liabilityId,amount,defaultDate));
        }
        return transactionList;
    }


    // =======================
    // Utilidades
    // =======================

    private void upsertLiability(Long userId, Long liabilityTypeId, String name, Double outstandingBalance) {
        try {
            // Buscar si existe la liability
            Long liabilityId = jdbcTemplate.queryForObject(SQL_SELECT_LIABILITY, Long.class, userId, name);
            // Si existe, actualizar
            jdbcTemplate.update(SQL_UPDATE_LIABILITY, outstandingBalance, liabilityId);
        } catch (DataAccessException e) {
            // Si no existe, insertar
            jdbcTemplate.queryForObject(SQL_INSERT_LIABILITY, Long.class, userId, liabilityTypeId, name, outstandingBalance);
        }
    }

    private void upsertAsset(Long userId, Long assetTypeId, String name, Double currentValue) {
        try {
            // Buscar si existe el asset
            Long assetId = jdbcTemplate.queryForObject(SQL_SELECT_ASSET, Long.class, userId, name);
            // Si existe, actualizar
            jdbcTemplate.update(SQL_UPDATE_ASSET, currentValue, assetId);
        } catch (DataAccessException e) {
            // Si no existe, insertar
            jdbcTemplate.queryForObject(SQL_INSERT_ASSET, Long.class, userId, assetTypeId, name, currentValue);
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
