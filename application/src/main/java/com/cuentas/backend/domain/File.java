package com.cuentas.backend.domain;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {

    private Long id;
    private Long userId;
    private Long fileTypeId;   // En Domain usamos solo el id
    private Integer year;
    private LocalDateTime uploadDate;
    private String fileName;
    private Long fileSize;
    private byte[] fileData;
}