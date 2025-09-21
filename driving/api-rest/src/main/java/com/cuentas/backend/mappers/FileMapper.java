package com.cuentas.backend.mappers;


import com.cuentas.backend.domain.File;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class FileMapper {

    public static File toDomain(MultipartFile multipartFile, Long userId, int fileType, int year) {
        try {
            File file = new File();
            file.setUserId(userId);
            file.setFileTypeId(Long.valueOf(fileType));
            file.setYear(year);
            file.setUploadDate(LocalDateTime.now());
            file.setFileName(multipartFile.getOriginalFilename());
            file.setFileSize(multipartFile.getSize());
            file.setFileData(multipartFile.getBytes());
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el contenido del fichero", e);
        }
    }
}