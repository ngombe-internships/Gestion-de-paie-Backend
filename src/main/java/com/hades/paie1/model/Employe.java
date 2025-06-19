package com.hades.paie1.model;

import com.hades.paie1.enum1.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "employes")
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
    private CiviliteEnum civilite;



}
