package com.hades.paie1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfExportRequestDto {
    private String from;
    private String to;
    private String email;
}
