package com.hades.maalipo.dto.entreprise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseParametreRhDto {
    private Long id;
    private Long entrepriseId;
    private String cleParametre;
    private String valeurParametre;
    private String description;
}