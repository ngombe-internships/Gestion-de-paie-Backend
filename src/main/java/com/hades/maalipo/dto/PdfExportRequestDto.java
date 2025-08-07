package com.hades.maalipo.dto;

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
