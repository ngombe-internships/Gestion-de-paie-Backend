package com.hades.maalipo.dto.conge;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JourFerieUpdateDto {
    private Long id;
    private String nom;
    private LocalDate dateFerie;
    private Boolean estChomeEtPaye;
    private Long entrepriseId;

    // getters et setters
}