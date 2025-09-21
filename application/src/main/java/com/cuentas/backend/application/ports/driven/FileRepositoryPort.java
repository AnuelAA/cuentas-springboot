package com.cuentas.backend.application.ports.driven;

import com.cuentas.backend.domain.File;

public interface FileRepositoryPort {
    void saveFile(File file);
}

