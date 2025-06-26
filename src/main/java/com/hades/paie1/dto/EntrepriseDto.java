package com.hades.paie1.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntrepriseDto {
    private Long id;
    private String nom;
    private String adresseEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String numeroSiret;

    private String logoUrl;



}
