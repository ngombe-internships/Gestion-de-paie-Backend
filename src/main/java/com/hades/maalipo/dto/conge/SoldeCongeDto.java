package com.hades.maalipo.dto.conge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoldeCongeDto {
    private Long employeId;
    private String nom;
    private String prenom;
    private String matricule;
    private BigDecimal soldeAcquisTotal; // Nombre total de jours acquis
    private BigDecimal soldeDisponible; // Solde restant
    private BigDecimal soldePris; // Jours déjà pris
    private LocalDate dateEmbauche;
    private boolean enPeriodeEssai; // Indique si l'employé est en période d'essai
}