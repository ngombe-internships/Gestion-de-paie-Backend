package com.hades.paie1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.model.BulletinPaie;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulletinPaieResponseDto {
    private Long id;
    private BigDecimal tauxHoraire;
    private BigDecimal heuresNormal;


    private BigDecimal totalGains;
    private BigDecimal salaireBrut;
    private BigDecimal baseCnps;
    private BigDecimal salaireImposable;
    private BigDecimal avancesSurSalaires;
    private BigDecimal totalImpots;
    private BigDecimal totalRetenuesSalariales; // Somme de toutes les retenues salariales
    private BigDecimal totalChargesPatronales; // Somme de toutes les charges patronales
    private BigDecimal salaireNetAPayer;
    private BigDecimal cotisationCnps;
    private BigDecimal coutTotalEmployeur;

    // Informations générales du bulletin
    private LocalDate datePaiement;
    private StatusBulletin statusBulletin;
    private LocalDate dateCreationBulletin;
    private String periodePaie;
    private String methodePaiement;

    // Objets liés
    private EntrepriseDto entreprise;
    private EmployeResponseDto employe;

    // Liste dynamique des lignes de paie ***
    private List<LignePaieDto> lignesPaie;
}






