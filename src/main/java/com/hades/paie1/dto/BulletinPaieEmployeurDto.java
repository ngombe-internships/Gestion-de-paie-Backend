package com.hades.paie1.dto;

import com.hades.paie1.enum1.StatusBulletin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BulletinPaieEmployeurDto {
    private Long id;
    private EmployeResponseDto employe;
    private BigDecimal salaireBase;
    private BigDecimal tauxHoraire;
    private BigDecimal heuresNormal;
    private BigDecimal heuresSup1;
    private BigDecimal heuresSup2;
    private BigDecimal heuresNuit;
    private BigDecimal heuresFerie;
    private BigDecimal primeTransport;
    private BigDecimal primePonctualite;
    private BigDecimal primeTechnicite;
    private BigDecimal primeAnciennete;
    private BigDecimal primeRendement;
    private BigDecimal autrePrimes;
    private BigDecimal avantageNature;
    private BigDecimal totalPrimes;
    private BigDecimal salaireBrut;
    private BigDecimal salaireImposable;
    private BigDecimal baseCnps;
    private BigDecimal baseCnps1;
    private BigDecimal irpp;
    private BigDecimal cac;
    private BigDecimal taxeCommunale;
    private BigDecimal redevanceAudioVisuelle;
    private BigDecimal cnpsVieillesseSalarie;
    private BigDecimal creditFoncierSalarie;
    private BigDecimal fneSalarie;
    private BigDecimal totalRetenues;
    private BigDecimal cnpsVieillesseEmployeur;
    private BigDecimal cnpsAllocationsFamiliales;
    private BigDecimal cnpsAccidentsTravail;
    private BigDecimal creditFoncierPatronal;
    private BigDecimal fnePatronal;
    private BigDecimal totalChargesPatronales;
    private BigDecimal cotisationCnps;
    private BigDecimal coutTotalEmployeur;
    private BigDecimal salaireNet;
    private LocalDateTime dateCreation;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private Integer jourConge;
    private String mois;
    private Integer annee;

    private String periodePaie;
    private LocalDate dateCreationBulletin;
    private LocalDate datePaiement;
    private StatusBulletin statusBulletin;

    private BigDecimal primeExceptionnelle;


}