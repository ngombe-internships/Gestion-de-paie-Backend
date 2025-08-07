package com.hades.maalipo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployePaieConfigDto {
    private Long id;
    private Long employe;
    private Long elementPaie;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private BigDecimal taux; // Pour les POURCENTAGE_BASE

   private BigDecimal montant;
    private BigDecimal nombre;
}
