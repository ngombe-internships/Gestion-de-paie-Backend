package com.hades.maalipo.model;

import com.fasterxml.jackson.annotation.*;
import com.hades.maalipo.enum1.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employes")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Employe {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricule_interne", unique = true, nullable = false)
    private String matricule;

    @Column(name = "nom",nullable = false)
    private String nom;

    @Column(name = "prenom",nullable = false)
    private String prenom;

    @Column (name="numero_cnps", unique = true)
    private String numeroCnps;

    @Column(name="niu", unique = true)
    private String niu;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    @Email(message="Adresse email invalide")
    @NotBlank(message="Adresse email invalide")
    private String email;

    @Column(name = "adresse")
    private String adresse;

    @Builder.Default
    @Column(name = "actif", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean actif = true;



    @Column(name = "poste")
    private String poste;

    @Column(name = "service")
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificationProfessionnelle")
    private StatutEmployeEnum classificationProfessionnelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "echelonEnum")
    private EchelonEnum echelonEnum;

    @Enumerated(EnumType.STRING)
    @Column (name="categorieEnum")
    private CategorieEnum categorieEnum;

    @Column(name="date_embauche")
    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    @Column(name="type_Contrat")
    private TypeContratEnum typeContratEnum;

    @Column(name="date_naissance")
    private  LocalDate dateNaissance;

    @Column(name="sexe")
    private SexeEnum sexe;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    //@JsonBackReference("user-employe")
    @JsonIdentityReference(alwaysAsId = true)
    private  User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
   // @JsonBackReference("employe-entreprise")
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private Entreprise entreprise;


    // Nouveau pour conge et temps
    @Column(name = "soldeJoursConge")
    private BigDecimal soldeJoursConge;

    @Column(name="heures_contractuelles_hebdomadaires", precision = 5, scale=2)
    private BigDecimal heuresContractuellesHebdomadaires;

    @Column(name="jours_ouvrable_contractuels_hebdomadaires")
    private Integer joursOuvrablesContractuelsHebdomadaires;

    @Column(name="salaire_base")
    private  BigDecimal salaireBase;


    @OneToMany(mappedBy = "employe")
   // @JsonManagedReference("employe-avantages")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private List<EmployeAvantageNature> avantagesNature = new ArrayList<>();



}
