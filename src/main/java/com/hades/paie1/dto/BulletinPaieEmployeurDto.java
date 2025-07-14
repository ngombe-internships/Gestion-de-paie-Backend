package com.hades.paie1.dto;

import com.hades.paie1.enum1.StatusBulletin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaieEmployeurDto {
    private Long id;
    private BigDecimal salaireBaseInitial;
    private BigDecimal tauxHoraireInitial;
    private BigDecimal heuresNormal;
    private BigDecimal heuresSup;
    private BigDecimal heuresNuit;
    private BigDecimal heuresFerie;
    private BigDecimal avancesSurSalaires;

    private BigDecimal  totalGains;
    private BigDecimal salaireBrut;
    private BigDecimal salaireImposable;
    private BigDecimal baseCnps;
    private BigDecimal totalRetenuesSalariales;
    private BigDecimal totalImpots;
    private BigDecimal totalChargesPatronales;
    private BigDecimal cotisationCnps;
    private BigDecimal coutTotalEmployeur;
    private BigDecimal salaireNetAPayer;

    // Informations générales du bulletin
    private LocalDate datePaiement;
    private StatusBulletin statusBulletin;
    private LocalDate dateCreationBulletin;
    private String periodePaie;
    private String methodePaiement;

    // Objets liés
    private EmployeResponseDto employe;

    private List<LignePaieDto> lignesPaie;

}