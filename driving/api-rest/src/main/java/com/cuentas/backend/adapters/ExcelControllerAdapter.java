package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.ExcelNewServicePort;
import com.cuentas.backend.application.ports.driving.ExcelServicePort;
import com.cuentas.backend.domain.File;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Excel", description = "Endpoints para importar y procesar archivos Excel")
@RestController
@RequestMapping("/api/users/{userId}/excel")
public class ExcelControllerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExcelControllerAdapter.class);

    private final ExcelServicePort excelServicePort;

    private final ExcelNewServicePort excelNewServicePort;

    public ExcelControllerAdapter(ExcelServicePort excelServicePort, ExcelNewServicePort excelNewServicePort) {
        this.excelServicePort = excelServicePort;
        this.excelNewServicePort = excelNewServicePort;
    }

    @Operation(summary = "Importar y procesar Excel", description = "Importa un archivo Excel con matrices de ingresos, gastos, activos y pasivos para un usuario y año específicos")
    @PostMapping("/import")
    public ResponseEntity<?> importExcel(
            @PathVariable("userId") long userId,
            @RequestParam("year") int year,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            log.warn("Usuario {} intentó subir un Excel vacío", userId);
            return ResponseEntity.badRequest().body("El archivo Excel no puede estar vacío");
        }

        try {
            File domainFile = new File();
            domainFile.setFileData(file.getBytes());
            domainFile.setFileName(file.getOriginalFilename());
            log.info("Usuario {} subió Excel [{}], tamaño={} bytes", userId, file.getOriginalFilename(), file.getBytes().length);

            excelServicePort.processExcel(domainFile, year, userId);
            log.info("Procesado Excel correctamente para usuario {} año {}", userId, year);

            return ResponseEntity.ok("Archivo Excel procesado correctamente");
        } catch (IOException e) {
            log.error("Error leyendo el archivo Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo leer el archivo Excel");
        } catch (RuntimeException e) {
            log.error("Error procesando el Excel para usuario {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando el archivo Excel: " + e.getMessage());
        }
    }

    @Operation(summary = "Importar y procesar Excel", description = "Importa un archivo Excel con matrices de ingresos, gastos, activos y pasivos para un usuario y año específicos")
    @PostMapping("/importNew")
    public ResponseEntity<?> importExcelNew(
            @PathVariable("userId") long userId,
            @RequestParam("year") int year,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            log.warn("Usuario {} intentó subir un Excel vacío", userId);
            return ResponseEntity.badRequest().body("El archivo Excel no puede estar vacío");
        }

        try {
            File domainFile = new File();
            domainFile.setFileData(file.getBytes());
            domainFile.setFileName(file.getOriginalFilename());
            log.info("Usuario {} subió Excel [{}], tamaño={} bytes", userId, file.getOriginalFilename(), file.getBytes().length);

            excelNewServicePort.processExcel(domainFile, year, userId);
            log.info("Procesado Excel correctamente para usuario {} año {}", userId, year);

            return ResponseEntity.ok("Archivo Excel procesado correctamente");
        } catch (IOException e) {
            log.error("Error leyendo el archivo Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo leer el archivo Excel");
        } catch (RuntimeException e) {
            log.error("Error procesando el Excel para usuario {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando el archivo Excel: " + e.getMessage());
        }
    }
}