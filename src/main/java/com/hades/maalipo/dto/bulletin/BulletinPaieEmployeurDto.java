package com.hades.maalipo.dto.bulletin;

import com.hades.maalipo.dto.employe.EmployeResponseDto;
import com.hades.maalipo.enum1.StatusBulletin;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinPaieEmployeurDto {
    private Long id;

    @NotNull(message = "Le salaire de base est obligatoire")
    @Positive(message = "Le salaire doit être positif")
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
    @NotNull(message = "La date de paiement est obligatoire")
    @FutureOrPresent(message = "La date de paiement doit être présente ou future")
    private LocalDate datePaiement;

    private StatusBulletin statusBulletin;

    private LocalDate dateCreationBulletin;
    @NotEmpty(message = "La période de paie est obligatoire")
    @Size(max = 50)
    private String periodePaie;

    private String methodePaiement;

    // Objets liés
    @NotNull(message = "L'employé est obligatoire")
    private EmployeResponseDto employe;

    private List<LignePaieDto> lignesPaie;

}