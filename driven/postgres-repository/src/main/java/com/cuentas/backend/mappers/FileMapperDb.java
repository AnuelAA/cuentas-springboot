package com.cuentas.backend.mappers;

import com.cuentas.backend.domain.File;
import com.cuentas.backend.models.FileMO;
import org.springframework.stereotype.Component;

@Component
public class FileMapperDb {

    public static FileMO toFileMO(File file) {
        if (file == null) return null;

        FileMO fileMO = new FileMO();
        fileMO.setUserId(file.getUserId());
        fileMO.setFileTypeId(file.getFileTypeId());
        fileMO.setYear(file.getYear());
        fileMO.setUploadDate(file.getUploadDate());
        fileMO.setFileName(file.getFileName());
        fileMO.setFileSize(file.getFileSize());
        fileMO.setFileData(file.getFileData());
        return fileMO;
    }

    public static File toDomain(FileMO fileMO) {
        if (fileMO == null) return null;

        File file = new File();
        file.setId(fileMO.getId());
        file.setUserId(fileMO.getUserId());
        file.setFileTypeId(fileMO.getFileTypeId());
        file.setYear(fileMO.getYear());
        file.setUploadDate(fileMO.getUploadDate());
        file.setFileName(fileMO.getFileName());
        file.setFileSize(fileMO.getFileSize());
        file.setFileData(fileMO.getFileData());
        return file;
    }
}
