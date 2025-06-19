package com.hades.paie1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data

@AllArgsConstructor
@NoArgsConstructor
@Table (name = "bulletin_paie")
public class BulletinPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    // Donne d'entree
    @Column(name= "salaire_base", precision = 15, scale = 2)
    private BigDecimal salaireBase;

    @Column(name="taux_horaire", precision = 15, scale = 2)
    private BigDecimal tauxHoraire;

    //Heure de travaille
    @Column(name="heures_normal", precision = 15, scale = 2)
    private  BigDecimal heuresNormal ;

    @Column(name="heures_sup1", precision = 15, scale = 2)
    private BigDecimal heuresSup1;

    @Column(name = "heures_sup2", precision = 15, scale = 2)
    private BigDecimal heuresSup2;

    @Column(name="heures_nuit", precision = 15, scale = 2)
    private BigDecimal heuresNuit;

    @Column(name="heures_ferie", precision = 15, scale = 2)
    private BigDecimal heuresFerie;


    @Column(name="date_embauche")
    private LocalDate dateEmbauche ;
    //Primes
    @Column(name= "prime_transport", precision = 15, scale = 2)
    private BigDecimal primeTransport;

    @Column(name= "prime_ponctualite", precision = 15, scale = 2)
    private BigDecimal primePonctualite;

    @Column(name= "prime_technicite", precision = 15, scale = 2)
    private BigDecimal primeTechnicite;

    @Column(name= "prime_anciennete", precision = 15, scale = 2)
    private BigDecimal primeAnciennete;

    @Column(name= "prime_rendement", precision = 15, scale = 2)
    private BigDecimal primeRendement;

    @Column(name= "autre_primes", precision = 15, scale = 2)
    private BigDecimal autrePrimes;

    //Avantage en Nature

    @Column(name="avantage_nature", precision = 15, scale = 2)
    private  BigDecimal avantageNature;


    // Résultats de calcul
    @Column(name = "salaire_brut", precision = 15, scale = 2)
    private BigDecimal salaireBrut;

    @Column(name = "salaire_net", precision = 15, scale = 2)
    private BigDecimal salaireNet;

    @Column(name = "total_charges_patronales", precision = 15, scale = 2)
    private BigDecimal totalChargesPatronales;

    // Retenues salariales
    @Column(name = "cnps_vieillesse", precision = 15, scale = 2)
    private BigDecimal cnpsVieillesse;

    @Column(name = "irpp", precision = 15, scale = 2)
    private BigDecimal irpp;





    @Column(name ="cac")
    private BigDecimal cac;

    @Column(name ="taxeCommunale")
    private BigDecimal taxeCommunale;

    @Column(name ="redevanceAudioVisuelle")
    private BigDecimal redevanceAudioVisuelle;

    // === SECTION COTISATIONS SALARIALES (RETENUES) ===
    @Column(name ="cnpsVieillesseSalarie")
    private BigDecimal cnpsVieillesseSalarie;

    @Column(name ="creditFoncierSalarie")
    private BigDecimal creditFoncierSalarie;

    @Column(name ="fneSalarie")
    private BigDecimal fneSalarie;

    @Column(name ="totalRetenues")
    private BigDecimal totalRetenues;

    // === SECTION CHARGES PATRONALES ===
    @Column(name ="cnpsVieillesseEmployeur")
    private BigDecimal cnpsVieillesseEmployeur;

    @Column(name ="cnpsAllocationsFamiliales")
    private BigDecimal cnpsAllocationsFamiliales;

    @Column(name ="cnpsAccidentsTravail")
    private BigDecimal cnpsAccidentsTravail;

    @Column(name ="creditFoncierPatronal")
    private BigDecimal creditFoncierPatronal;

    @Column(name ="fnePatronal")
    private BigDecimal fnePatronal;


    @Column(name ="coutTotalEmployeur")
    private BigDecimal coutTotalEmployeur;

    @Column(name = "cotisationCnps")
    private BigDecimal cotisationCnps;



    private BigDecimal totalPrimes;
    private BigDecimal totalGains;

    private BigDecimal salaireImposable;
    private BigDecimal BaseCnps;







}
