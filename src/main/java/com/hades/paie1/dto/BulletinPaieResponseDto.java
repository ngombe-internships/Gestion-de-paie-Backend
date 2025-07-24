package com.hades.paie1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hades.paie1.enum1.StatusBulletin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulletinPaieResponseDto {
    private Long id;
    private BigDecimal salaireBaseInitial;
    private BigDecimal tauxHoraire;
    private BigDecimal heuresNormal;
    private BigDecimal heuresSup;
    private BigDecimal heuresNuit;
    private BigDecimal heuresFerie;
    private BigDecimal primeAnciennete;
    private BigDecimal avancesSurSalaires;

    private BigDecimal totalGains;
    private BigDecimal salaireBrut;
    private BigDecimal salaireImposable;
    private BigDecimal baseCnps;
    private BigDecimal totalRetenuesSalariales;
    private BigDecimal totalImpots;
    private BigDecimal totalChargesPatronales;
    private BigDecimal cotisationCnps;
    private BigDecimal coutTotalEmployeur;
    private BigDecimal salaireNetAPayer;
    private BigDecimal salaireNetAvantImpot;
    // Informations générales
    private LocalDate datePaiement;
    private StatusBulletin statusBulletin;
    private LocalDate dateCreationBulletin;
    private String periodePaie;
    private String methodePaiement;

    // Objets liés
    private EntrepriseDto entreprise;
    private EmployeResponseDto employe;

    // Lignes dynamiques du bulletin
    private List<LignePaieDto> lignesPaie;
}