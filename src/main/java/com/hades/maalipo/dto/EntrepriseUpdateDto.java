package com.hades.maalipo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseUpdateDto {

    private String nom;
    private String adresseEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String logoUrl;
}
