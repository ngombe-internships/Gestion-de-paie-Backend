package com.hades.maalipo.dto.conge;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentDto {
    private Long id;
    private String nom;
    private String type;
    private String contentType;
    private Long taille;
    private LocalDateTime dateUpload;
    private String uploadedBy;
    private String url;
}