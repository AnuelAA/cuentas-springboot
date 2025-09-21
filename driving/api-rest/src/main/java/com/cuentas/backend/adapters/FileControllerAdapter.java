package com.cuentas.backend.adapters;


import com.cuentas.backend.application.ports.driving.ExcelServicePort;
import com.cuentas.backend.mappers.FileMapper;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.FilesApi;
import org.openapitools.model.FileUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping
public class FileControllerAdapter implements FilesApi {

    private final FileMapper fileMapper;
    private final ExcelServicePort excelServicePort;

    public FileControllerAdapter(FileMapper fileMapper, ExcelServicePort excelServicePort) {
        this.fileMapper = fileMapper;
        this.excelServicePort = excelServicePort;
    }

    @Override
    public ResponseEntity<FileUploadResponse> filesUploadPost(MultipartFile multipartFile) {

        String fileName = multipartFile.getOriginalFilename();
        var year = Integer.parseInt(fileName.replaceAll("\\D+", "").substring(0, 4));
        var file = fileMapper.toDomain(multipartFile, 1L, 1,year);
        excelServicePort.processExcel(file, 2025, 1);

        return ResponseEntity.ok(new FileUploadResponse());

    }
}
