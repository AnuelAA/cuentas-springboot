package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.*;
import com.cuentas.backend.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class MCPControllerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MCPControllerAdapter.class);

    private final AssetServicePort assetService;
    private final CategoryServicePort categoryService;
    private final LiabilityServicePort liabilityService;
    private final TransactionServicePort transactionService;
    private final UserServicePort userService;
    private final DashboardServicePort dashboardService;
    private final ExcelServicePort excelService;
    private final ExcelNewServicePort excelNewServicePort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPControllerAdapter(
            AssetServicePort assetService,
            CategoryServicePort categoryService,
            LiabilityServicePort liabilityService,
            TransactionServicePort transactionService,
            UserServicePort userService,
            DashboardServicePort dashboardService,
            ExcelServicePort excelService,
            ExcelNewServicePort excelNewServicePort){
        this.assetService = assetService;
        this.categoryService = categoryService;
        this.liabilityService = liabilityService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.dashboardService = dashboardService;
        this.excelService = excelService;
        this.excelNewServicePort = excelNewServicePort;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleRpc(@RequestBody Map<String, Object> request) {
        logger.info("Solicitud RPC recibida: {}", request);
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Object result;

        try {
            switch (method) {
                // USERS
                case "getUserById":
                case "getUser":
                    result = userService.getUserById(((Number) params.get("userId")).longValue());
                    break;
                case "updateUser":
                    result = userService.updateUser(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("user"), User.class));
                    break;
                case "deleteUser":
                    userService.deleteUser(((Number) params.get("userId")).longValue());
                    result = Map.of("deleted", true);
                    break;
                case "getAllUsers":
                    result = userService.getAllUsers();
                    break;
                case "createUser":
                    result = userService.createUser(toObject(params.get("user"), User.class));
                    break;

                // USER SETTINGS
                case "getUserSettings":
                    result = userService.getUserSettings(((Number) params.get("userId")).longValue());
                    break;
                case "updateUserSettings":
                    result = userService.updateUserSettings(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("settings"), UserSettings.class));
                    break;

                // ASSETS
                case "createAsset":
                    result = assetService.createAsset(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("asset"), Asset.class));
                    break;
                case "getAsset":
                    result = assetService.getAsset(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue());
                    break;
                case "listAssets":
                    result = assetService.listAssets(
                            ((Number) params.get("userId")).longValue());
                    break;
                case "updateAsset":
                    result = assetService.updateAsset(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue(),
                            toObject(params.get("asset"), Asset.class));
                    break;
                case "deleteAsset":
                    assetService.deleteAsset(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue());
                    result = Map.of("deleted", true);
                    break;

                // CATEGORIES
                case "createCategory":
                    result = categoryService.createCategory(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("category"), Category.class));
                    break;
                case "getCategory":
                    result = categoryService.getCategory(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("categoryId")).longValue());
                    break;
                case "listCategories":
                    result = categoryService.listCategories(
                            ((Number) params.get("userId")).longValue());
                    break;
                case "updateCategory":
                    result = categoryService.updateCategory(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("categoryId")).longValue(),
                            toObject(params.get("category"), Category.class));
                    break;
                case "deleteCategory":
                    categoryService.deleteCategory(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("categoryId")).longValue());
                    result = Map.of("deleted", true);
                    break;

                // LIABILITIES
                case "createLiability":
                    result = liabilityService.createLiability(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("liability"), Liability.class));
                    break;
                case "getLiability":
                    result = liabilityService.getLiability(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("liabilityId")).longValue());
                    break;
                case "listLiabilities":
                    result = liabilityService.listLiabilities(
                            ((Number) params.get("userId")).longValue());
                    break;
                case "updateLiability":
                    result = liabilityService.updateLiability(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("liabilityId")).longValue(),
                            toObject(params.get("liability"), Liability.class));
                    break;
                case "deleteLiability":
                    liabilityService.deleteLiability(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("liabilityId")).longValue());
                    result = Map.of("deleted", true);
                    break;

                // TRANSACTIONS
                case "createTransaction":
                    result = transactionService.createTransaction(
                            ((Number) params.get("userId")).longValue(),
                            toObject(params.get("transaction"), Transaction.class));
                    break;
                case "getTransaction":
                    result = transactionService.getTransaction(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("transactionId")).longValue());
                    break;
                case "listTransactions":
                    result = transactionService.listTransactions(
                            ((Number) params.get("userId")).longValue(),
                            toLocalDate(params.get("startDate")),
                            toLocalDate(params.get("endDate")),
                            toLong(params.get("liabilityId")),
                            toLong(params.get("assetId")),
                            toLong(params.get("categoryId")),
                            toLong(params.get("relatedAssetId")));
                    break;
                case "updateTransaction":
                    result = transactionService.updateTransaction(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("transactionId")).longValue(),
                            toObject(params.get("transaction"), Transaction.class));
                    break;
                case "deleteTransaction":
                    transactionService.deleteTransaction(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("transactionId")).longValue());
                    result = Map.of("deleted", true);
                    break;

                // DASHBOARD
                case "getDashboard":
                    result = dashboardService.getMetrics(
                            ((Number) params.get("userId")).longValue(),
                            toLocalDate(params.get("startDate")),
                            toLocalDate(params.get("endDate")));
                    break;
                case "getPeriodSummary":
                    Long uid = ((Number) params.get("userId")).longValue();
                    String period = params.get("period") != null ? params.get("period").toString() : null;
                    result = dashboardService.getPeriodSummary(uid, period);
                    break;
                case "getMonthlySummary":
                    result = dashboardService.getMonthlySummary(((Number) params.get("userId")).longValue(),
                            toInteger(params.get("year")));
                    break;
                case "getLiabilityProgress":
                    result = dashboardService.getLiabilityProgress(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("liabilityId")).longValue());
                    break;
                case "getAssetPerformance":
                    result = dashboardService.getAssetPerformance(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue(),
                            toLocalDate(params.get("startDate")),
                            toLocalDate(params.get("endDate")));
                    break;

                // ASSET ROI
                case "calculateAssetRoi":
                case "calculateAssetROI":
                    result = assetService.calculateAssetROI(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue(),
                            toLocalDate(params.get("startDate")),
                            toLocalDate(params.get("endDate")));
                    break;
                case "getMonthlyRoi":
                case "getMonthlyROI":
                    result = assetService.calculateMonthlyROI(
                            ((Number) params.get("userId")).longValue(),
                            ((Number) params.get("assetId")).longValue(),
                            toInteger(params.get("year")));
                    break;

                // EXCEL
                case "importExcel": {
                    com.cuentas.backend.domain.File excelFile = toObject(params.get("file"), com.cuentas.backend.domain.File.class);
                    excelService.processExcel(
                            excelFile,
                            ((Number) params.get("year")).intValue(),
                            ((Number) params.get("userId")).longValue()
                    );
                    result = Map.of("processed", true);
                    break;
                }
                case "importExcelNew": {
                    com.cuentas.backend.domain.File excelFileNew = toObject(params.get("file"), com.cuentas.backend.domain.File.class);
                    excelNewServicePort.processExcel(
                            excelFileNew,
                            ((Number) params.get("year")).intValue(),
                            ((Number) params.get("userId")).longValue()
                    );
                    result = Map.of("processed", true);
                    break;
                }
                // DEFAULT
                default:
                    ResponseEntity<Map<String, Object>> responseNotFound = ResponseEntity.badRequest().body(Map.of(
                            "jsonrpc", "2.0",
                            "id", request.get("id"),
                            "error", Map.of("code", -32601, "message", "Method not found")
                    ));
                    logger.info("Respuesta RPC: {}", responseNotFound);
                    return responseNotFound;
            }

            ResponseEntity<Map<String, Object>> responseOk = ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", request.get("id"),
                    "result", result
            ));
            logger.info("Respuesta RPC: {}", responseOk);
            return responseOk;

        } catch (Exception e) {
            logger.error("Error en RPC: {}", e.getMessage(), e);
            ResponseEntity<Map<String, Object>> responseError = ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", request.get("id"),
                    "error", Map.of("code", -32603, "message", e.getMessage())
            ));
            logger.info("Respuesta RPC: {}", responseError);
            return responseError;
        }
    }

    // Helpers
    private <T> T toObject(Object raw, Class<T> clazz) {
        return objectMapper.convertValue(raw, clazz);
    }

    private LocalDate toLocalDate(Object raw) {
        return raw != null ? LocalDate.parse(raw.toString()) : null;
    }

    private Long toLong(Object raw) {
        return raw != null ? ((Number) raw).longValue() : null;
    }

    private Integer toInteger(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) return ((Number) raw).intValue();
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<YearMonth> toYearMonthList(Object raw) {
        if (raw == null) return null;
        try {
            if (raw instanceof List) {
                List<?> list = (List<?>) raw;
                List<YearMonth> res = new ArrayList<>();
                for (Object o : list) {
                    if (o != null) res.add(YearMonth.parse(o.toString()));
                }
                return res;
            } else {
                List<YearMonth> res = new ArrayList<>();
                res.add(YearMonth.parse(raw.toString()));
                return res;
            }
        } catch (DateTimeParseException ex) {
            logger.warn("Formato de months inválido: {}, se ignorará. Esperado YYYY-MM", raw);
            return null;
        }
    }
}
