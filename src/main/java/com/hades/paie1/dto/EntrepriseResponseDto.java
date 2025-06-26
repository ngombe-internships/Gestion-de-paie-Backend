package com.hades.paie1.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EntrepriseResponseDto {
    private Long id;
    private String nom;
    private String adresseEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String numeroSiret;
    private LocalDate dateCreation;
    private String logoUrl;
}