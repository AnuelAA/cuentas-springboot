package com.cuentas.backend.adapters;

import com.cuentas.backend.FileMOJpaRepository;
import com.cuentas.backend.application.ports.driven.FileRepositoryPort;
import com.cuentas.backend.domain.File;
import com.cuentas.backend.mappers.FileMapperDb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileRepositoryAdapter implements FileRepositoryPort {

    private final FileMapperDb fileMapperDb;
    private final FileMOJpaRepository fileMOJpaRepository;

    public FileRepositoryAdapter(FileMapperDb fileMapperDb, FileMOJpaRepository fileMOJpaRepository) {
        this.fileMapperDb = fileMapperDb;
        this.fileMOJpaRepository = fileMOJpaRepository;
    }

    @Override
  public void saveFile (File file) {
        System.out.println("fileData type: " + (file.getFileData() != null ? file.getFileData().getClass() : null));
        System.out.println("fileSize type: " + (file.getFileSize() != null ? file.getFileSize().getClass() : null));

        //fileMOJpaRepository.save(fileMapperDb.toFileMO(file));
      //log.info("File saved successfully" );
  }
}
