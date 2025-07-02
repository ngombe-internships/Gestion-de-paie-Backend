package com.hades.paie1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseDto {
    private Long id;
    private String nom;
    private String adresseEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String numeroSiret;
    private String logoUrl;
    private LocalDate dateCreation;

    private Long employeurPrincipalId;
    private String employeurPrincipalUsername;


}
