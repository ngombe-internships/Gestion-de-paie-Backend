package com.hades.paie1.model;

import com.fasterxml.jackson.annotation.*;
import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.MethodePaiement;
import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.enum1.TypeElementPaie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data

@AllArgsConstructor
@NoArgsConstructor
@Table (name = "bulletin_paie")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BulletinPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="entreprise_id", nullable = false)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private  Entreprise entreprise;

    //sert pour ajouter la liste des lignes de paie
    @OneToMany(mappedBy = "bulletinPaie", cascade = CascadeType.ALL, orphanRemoval = true )
    @JsonManagedReference("bulletin-lignes")
    private List<LigneBulletinPaie> lignesPaie = new ArrayList<>();


    // Donne d'entree
    @Column(name= "salaire_base_initial", precision = 15, scale = 2)
    private BigDecimal salaireBaseInitial;

    @Column(name="taux_horaire_initial", precision = 15, scale = 2)
    private BigDecimal tauxHoraireInitial;

    //Heure de travaille
    @Column(name="heures_normal", precision = 15, scale = 2)
    private  BigDecimal heuresNormal ;

    @Column(name="heures_sup", precision = 15, scale = 2)
    private BigDecimal heuresSup;

    @Column(name="heures_nuit", precision = 15, scale = 2)
    private BigDecimal heuresNuit;

    @Column(name="heures_ferie", precision = 15, scale = 2)
    private BigDecimal heuresFerie;


    //Primes
    @Column(name= "prime_anciennete", precision = 15, scale = 2)
    private BigDecimal primeAnciennete;



    //cest champs seront calcul et mis a jour avant la sauvegarde
    @Column(name = "total_gains", precision = 15, scale = 2)
    private BigDecimal totalGains = BigDecimal.ZERO;

    @Column(name = "total_retenues_salariales", precision = 15, scale = 2)
    private BigDecimal totalRetenuesSalariales = BigDecimal.ZERO;

    @Column(name = "total_charges_patronales", precision = 15, scale = 2)
    private BigDecimal totalChargesPatronales = BigDecimal.ZERO;

    @Column(name = "salaire_brut", precision = 15, scale = 2)
    private BigDecimal salaireBrut = BigDecimal.ZERO;

    @Column(name = "salaire_net_avant_impot", precision = 15, scale = 2) // Nouveau champ pour clarté
    private BigDecimal salaireNetAvantImpot = BigDecimal.ZERO;

    @Column(name = "salaire_net_a_payer", precision = 15, scale = 2)
    private BigDecimal salaireNetAPayer = BigDecimal.ZERO;

    @Column(name = "total_impots", precision = 15, scale = 2)
    private BigDecimal totalImpots = BigDecimal.ZERO; // Somme des éléments de type Impôt

    @Column(name = "total_cotisations_salariales", precision = 15, scale = 2)
    private BigDecimal totalCotisationsSalariales = BigDecimal.ZERO;

    @Column(name = "salaire_imposable", precision = 15, scale = 2)
    private BigDecimal salaireImposable = BigDecimal.ZERO;

    @Column(name = "base_cnps", precision = 15, scale = 2)
    private BigDecimal baseCnps = BigDecimal.ZERO;

    @Column(name = "cout_total_employeur", precision = 15, scale = 2)
    private BigDecimal coutTotalEmployeur = BigDecimal.ZERO;

    @Column(name = "cotisation_cnps", precision = 15, scale = 2)
    private BigDecimal cotisationCnps = BigDecimal.ZERO;

    @Column(name = "annee")
    private Integer annee;
    @Column(name = "mois")
    private  String mois;

    @Column(name = "date_debut_periode")
    private LocalDate dateDebutPeriode;




    @Enumerated(EnumType.STRING)
    @Column(name = "statusBulletin")
    private StatusBulletin statusBulletin;

    @Column(name = "periodePaie")
    private String periodePaie;

    @Column(name = "dateCreation")
    private LocalDate dateCreationBulletin;

    @Column(name = "datePaiement")
    private  LocalDate datePaiement;

    @Enumerated(EnumType.ORDINAL)
    @Column(name="methodePaiement")
    private MethodePaiement methodePaiement;


    @Column(name = "referencePaiement", length = 100)
    private String referencePaiement;

    @Column(name = "avances_sur_salaires", precision = 15, scale = 2)
    private BigDecimal avancesSurSalaires;



    public void addLignePaie(LigneBulletinPaie ligne) {
        this.lignesPaie.add(ligne);
        ligne.setBulletinPaie(this); // Assurez-vous que le lien bidirectionnel est établi

        // Mettre à jour les totaux agrégés en fonction du type de ligne
        if (ligne.getElementPaie() != null) {
            // Mettre à jour les indicateurs estGain, estRetenue, estChargePatronale dans la ligne elle-même (si non déjà fait par le calculateur)
            ligne.setEstGain(ligne.getElementPaie().getType() == TypeElementPaie.GAIN);
            ligne.setEstRetenue(ligne.getElementPaie().getType() == TypeElementPaie.RETENUE);
            ligne.setEstChargePatronale(ligne.getElementPaie().getType() == TypeElementPaie.CHARGE_PATRONALE);
        }

        // Mise à jour de salaireNetAPayer : à revoir, sera calculé à la fin des calculs
        // Pour l'instant, on peut mettre à jour ici salaireNetAvantImpot
        // Le calcul final de salaireNetAPayer se fera dans BulletinPaieService ou le calculateur principal
    }

    // Méthode pour nettoyer les lignes existantes (utile avant un recalcul complet)
    public void clearLignesPaie() {
        this.lignesPaie.clear();
        this.totalGains = BigDecimal.ZERO;
        this.totalRetenuesSalariales = BigDecimal.ZERO;
        this.totalChargesPatronales = BigDecimal.ZERO;
        this.salaireBrut = BigDecimal.ZERO;
        this.salaireNetAvantImpot = BigDecimal.ZERO;
        this.salaireNetAPayer = BigDecimal.ZERO;
        this.totalImpots = BigDecimal.ZERO;
        this.totalCotisationsSalariales = BigDecimal.ZERO;
        this.salaireImposable = BigDecimal.ZERO;
        this.baseCnps = BigDecimal.ZERO;

        this.coutTotalEmployeur = BigDecimal.ZERO;
        this.cotisationCnps = BigDecimal.ZERO;
    }

}
