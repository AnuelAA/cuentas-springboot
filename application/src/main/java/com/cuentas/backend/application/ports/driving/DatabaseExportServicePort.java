package com.cuentas.backend.application.ports.driving;

public interface DatabaseExportServicePort {
    String exportDatabaseSchemaAndData(Long userId);
}

