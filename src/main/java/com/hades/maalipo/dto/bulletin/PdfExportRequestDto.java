package com.hades.maalipo.dto.bulletin;

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
