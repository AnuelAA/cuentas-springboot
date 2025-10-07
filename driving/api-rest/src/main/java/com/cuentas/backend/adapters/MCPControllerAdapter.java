package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.*;
import com.cuentas.backend.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

    public MCPControllerAdapter(
            AssetServicePort assetService,
            CategoryServicePort categoryService,
            LiabilityServicePort liabilityService,
            TransactionServicePort transactionService) {
        this.assetService = assetService;
        this.categoryService = categoryService;
        this.liabilityService = liabilityService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleRpc(@RequestBody Map<String, Object> request) {
        logger.info("Solicitud RPC recibida: {}", request);
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Object result;

        try {
            switch (method) {
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
        return new ObjectMapper().convertValue(raw, clazz);
    }

    private LocalDate toLocalDate(Object raw) {
        return raw != null ? LocalDate.parse(raw.toString()) : null;
    }

    private Long toLong(Object raw) {
        return raw != null ? ((Number) raw).longValue() : null;
    }
}
