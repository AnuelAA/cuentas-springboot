package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.File;

public interface ExcelServicePort {
    void processExcel(File excelFile, int year, long userId);
}
