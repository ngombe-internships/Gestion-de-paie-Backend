package com.hades.paie1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.model.BulletinPaie;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data

public class BulletinPaieResponseDto {

//    @JsonProperty("fiche_originale")
//    private BulletinPaie ficheOriginal;

    private Long id;
    //Section Gains
    @JsonProperty("tauxHoraire")
     private BigDecimal tauxHoraire;
    @JsonProperty("heuresNormal")
    private BigDecimal heuresNormal;
    @JsonProperty("salaireBase")
    private BigDecimal salaireBase;

    @JsonProperty("heuresSup1")
    private BigDecimal heureSup1;

    @JsonProperty("heuresSup2")
    private BigDecimal heureSup2;

    @JsonProperty("heuresNuit")
    private BigDecimal heureNuit;

    @JsonProperty("heuresFerie")
    private BigDecimal heureFerie;

    @JsonProperty("primeTransport")
    private BigDecimal primeTransport;

    @JsonProperty("primePonctualite")
    private BigDecimal primePonctualite;

    @JsonProperty("primeAnciennete")
    private BigDecimal primeAnciennete;

    @JsonProperty("primeRendement")
    private BigDecimal primeRendement;

    @JsonProperty("primeTechnicite")
    private BigDecimal primeTechnicite;

    @JsonProperty("totalPrimes")
    private BigDecimal totalPrimes;

    @JsonProperty("salaireBrut")
    private BigDecimal salaireBrut;

    @JsonProperty("salaireImposable")
    private BigDecimal salaireImposable;

    @JsonProperty("baseCnps")
    private BigDecimal baseCnps;


    //Section Impot et Taxe
    @JsonProperty("irpp")
    private BigDecimal irpp;

    @JsonProperty("cac")
    private BigDecimal cac;

    @JsonProperty("taxeCommunale")
    private BigDecimal taxeCommunale;

    @JsonProperty("redevanceAudioVisuelle")
    private BigDecimal redevanceAudioVisuelle;

    // === SECTION COTISATIONS SALARIALES (RETENUES) ===
    @JsonProperty("cnpsVieillesseSalarie")
    private BigDecimal cnpsVieillesseSalarie;

    @JsonProperty("creditFoncierSalarie")
    private BigDecimal creditFoncierSalarie;

    @JsonProperty("fneSalarie")
    private BigDecimal fneSalarie;

    @JsonProperty("totalRetenues")
    private BigDecimal totalRetenues;

    // === SECTION CHARGES PATRONALES ===
    @JsonProperty("cnpsVieillesseEmployeur")
    private BigDecimal cnpsVieillesseEmployeur;

    @JsonProperty("cnpsAllocationsFamiliales")
    private BigDecimal cnpsAllocationsFamiliales;

    @JsonProperty("cnpsAccidentsTravail")
    private BigDecimal cnpsAccidentsTravail;

    @JsonProperty("creditFoncierPatronal")
    private BigDecimal creditFoncierPatronal;

    @JsonProperty("fnePatronal")
    private BigDecimal fnePatronal;

    @JsonProperty("totalChargesPatronales")
    private BigDecimal totalChargesPatronales;

    // === SECTION TOTAUX FINAUX ===
    @JsonProperty("salaireNet")
    private BigDecimal salaireNet;

    @JsonProperty("coutTotalEmployeur")
    private BigDecimal coutTotalEmployeur;

    @JsonProperty("cotisationCnps")
    private BigDecimal cotisationCnps;

    @JsonProperty("periodePaie")
    private String periodePaie;

    @JsonProperty("ateCreationBulletin")
    private LocalDate dateCreationBulletin;

    @JsonProperty("statusBulletin")
    private StatusBulletin statusBulletin;

    @JsonProperty("datePaiement")
    private LocalDate datePaiement;


    private BigDecimal primeExceptionnellee;

    //entite employe et Entreprise
    @JsonProperty("employe")
    private  EmployeResponseDto employe;
    private EntrepriseDto entreprise;



//    @JsonProperty("allocationConge") // pour les calcul de la cnps on utilise ceci
//     private BigDecimal allocationConge;
//
//    @JsonProperty("jourConge")
//    private BigDecimal jourConge;
    // === SECTION Employe===




//    public BulletinPaie getFicheOriginal() {
//        return ficheOriginal;
//    }
//
//    public void setFicheOriginal(BulletinPaie ficheOriginal) {
//        this.ficheOriginal = ficheOriginal;
//    }


}




