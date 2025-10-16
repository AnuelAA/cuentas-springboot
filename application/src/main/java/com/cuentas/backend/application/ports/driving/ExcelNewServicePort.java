package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.File;

public interface ExcelNewServicePort {
    void processExcel(File excelFile, int year, long userId);
    byte[] exportExcel(int year, long userId);
}
