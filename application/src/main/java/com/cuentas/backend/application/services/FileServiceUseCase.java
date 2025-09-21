package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driven.FileRepositoryPort;
import com.cuentas.backend.application.ports.driving.FileServicePort;
import com.cuentas.backend.domain.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//@Slf4j
@Service
public class FileServiceUseCase implements FileServicePort {
  private final FileRepositoryPort repositoryPort;
    public FileServiceUseCase(FileRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }
  @Override
  public void saveFile(File file) {
    repositoryPort.saveFile(file);
  }
}
