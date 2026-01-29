package com.cuentas.backend.adapters;

import com.cuentas.backend.application.ports.driving.DatabaseExportServicePort;
import com.cuentas.backend.application.ports.driving.ExcelNewServicePort;
import com.cuentas.backend.application.ports.driving.ExcelServicePort;
import com.cuentas.backend.domain.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/users/{userId}/excel")
public class ExcelControllerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExcelControllerAdapter.class);

    private final ExcelServicePort excelServicePort;
    private final ExcelNewServicePort excelNewServicePort;
    private final DatabaseExportServicePort databaseExportServicePort;

    public ExcelControllerAdapter(ExcelServicePort excelServicePort, ExcelNewServicePort excelNewServicePort, DatabaseExportServicePort databaseExportServicePort) {
        this.excelServicePort = excelServicePort;
        this.excelNewServicePort = excelNewServicePort;
        this.databaseExportServicePort = databaseExportServicePort;
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(
            @PathVariable("userId") long userId,
            @RequestParam("year") int year,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Solicitud de importación de Excel para userId={}, year={}, file={}", userId, year, file != null ? file.getOriginalFilename() : null);

        if (file == null || file.isEmpty()) {
            log.warn("Usuario {} intentó subir un Excel vacío", userId);
            ResponseEntity<?> response = ResponseEntity.badRequest().body("El archivo Excel no puede estar vacío");
            log.info("Respuesta importExcel: {}", response);
            return response;
        }

        try {
            File domainFile = new File();
            domainFile.setFileData(file.getBytes());
            domainFile.setFileName(file.getOriginalFilename());
            log.info("Usuario {} subió Excel [{}], tamaño={} bytes", userId, file.getOriginalFilename(), file.getBytes().length);

            excelServicePort.processExcel(domainFile, year, userId);
            log.info("Procesado Excel correctamente para usuario {} año {}", userId, year);

            ResponseEntity<?> response = ResponseEntity.ok("Archivo Excel procesado correctamente");
            log.info("Respuesta importExcel: {}", response);
            return response;
        } catch (IOException e) {
            log.error("Error leyendo el archivo Excel: {}", e.getMessage(), e);
            ResponseEntity<?> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo leer el archivo Excel");
            log.info("Respuesta importExcel: {}", response);
            return response;
        } catch (RuntimeException e) {
            log.error("Error procesando el Excel para usuario {}: {}", userId, e.getMessage(), e);
            ResponseEntity<?> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando el archivo Excel: " + e.getMessage());
            log.info("Respuesta importExcel: {}", response);
            return response;
        }
    }

    @PostMapping("/importNew")
    public ResponseEntity<?> importExcelNew(
            @PathVariable("userId") long userId,
            @RequestParam("year") int year,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Solicitud de importación de Excel (nuevo) para userId={}, year={}, file={}", userId, year, file != null ? file.getOriginalFilename() : null);

        if (file == null || file.isEmpty()) {
            log.warn("Usuario {} intentó subir un Excel vacío", userId);
            ResponseEntity<?> response = ResponseEntity.badRequest().body("El archivo Excel no puede estar vacío");
            log.info("Respuesta importExcelNew: {}", response);
            return response;
        }

        try {
            File domainFile = new File();
            domainFile.setFileData(file.getBytes());
            domainFile.setFileName(file.getOriginalFilename());
            log.info("Usuario {} subió Excel [{}], tamaño={} bytes", userId, file.getOriginalFilename(), file.getBytes().length);

            excelNewServicePort.processExcel(domainFile, year, userId);
            log.info("Procesado Excel correctamente para usuario {} año {}", userId, year);

            ResponseEntity<?> response = ResponseEntity.ok("Archivo Excel procesado correctamente");
            log.info("Respuesta importExcelNew: {}", response);
            return response;
        } catch (IOException e) {
            log.error("Error leyendo el archivo Excel: {}", e.getMessage(), e);
            ResponseEntity<?> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo leer el archivo Excel");
            log.info("Respuesta importExcelNew: {}", response);
            return response;
        } catch (RuntimeException e) {
            log.error("Error procesando el Excel para usuario {}: {}", userId, e.getMessage(), e);
            ResponseEntity<?> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando el archivo Excel: " + e.getMessage());
            log.info("Respuesta importExcelNew: {}", response);
            return response;
        }
    }
    @GetMapping("/exportNew")
    public ResponseEntity<?> exportExcelNew(
            @PathVariable("userId") long userId,
            @RequestParam("year") int year
    ) {
        log.info("Solicitud de exportación Excel (nuevo) para userId={}, year={}", userId, year);
        try {
            byte[] fileBytes = excelNewServicePort.exportExcel(year, userId);
            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            // Formato: AAMMDD-cuentas_AAAA.xlsx (ejemplo: 241215-cuentas_2024.xlsx)
            LocalDate now = LocalDate.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMdd");
            String dateStr = now.format(dateFormatter);
            String filename = String.format("%s-cuentas_%d.xlsx", dateStr, year);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.setContentLength(fileBytes.length);
            log.info("Solicitud de exportación Excel (nuevo) CORRECTA para userId={}, year={}", userId, year);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exportando Excel para userId={}, year={}: {}", userId, year, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generando Excel: " + e.getMessage());
        }
    }

    @GetMapping("/exportDatabase")
    public ResponseEntity<?> exportDatabase(
            @PathVariable("userId") long userId
    ) {
        log.info("Solicitud de exportación de base de datos para userId={}", userId);
        try {
            String databaseContent = databaseExportServicePort.exportDatabaseSchemaAndData(userId);
            if (databaseContent == null || databaseContent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            
            byte[] fileBytes = databaseContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            // Formato: AAMMDD-database-export.txt (ejemplo: 241215-database-export.txt)
            LocalDate now = LocalDate.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMdd");
            String dateStr = now.format(dateFormatter);
            String filename = String.format("%s-database-export.txt", dateStr);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.setContentLength(fileBytes.length);
            
            log.info("Solicitud de exportación de base de datos CORRECTA para userId={}", userId);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exportando base de datos para userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generando exportación de base de datos: " + e.getMessage());
        }
    }
}
