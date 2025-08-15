package com.hades.maalipo.dto.conge;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JourFerieDto {
    private Long id;
    private LocalDate dateFerie;
    private String nom;
    private Boolean estChomeEtPaye;
    private Long entrepriseId;
    private String entrepriseNom; // Facultatif
}
