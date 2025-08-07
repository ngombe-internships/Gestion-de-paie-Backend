package com.hades.maalipo.dto;


import com.hades.maalipo.enum1.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeCreateDto {

    @NotBlank(message = "Le matricule interne ne peut pas être vide")
    private String matricule;

    @NotBlank(message = "Le nom ne peut pas être vide")
    private String nom;

    @NotBlank(message = "Le prénom ne peut pas être vide")
    private String prenom;

    @NotBlank(message = "Le numéro CNPS ne peut pas être vide")
    private String numeroCnps;

    @NotBlank(message = "Le NIU ne peut pas être vide")
    private String niu;

    @Email(message = "Adresse email invalide")
    @NotBlank(message = "L'adresse email ne peut pas être vide")
    private String email;

    private String adresse;

    private String telephone;

    @NotNull(message = "La date d'embauche est obligatoire")
    @PastOrPresent(message = "La date d'embauche ne peut pas être dans le futur")
    private LocalDate dateEmbauche;

    @NotNull(message = "Le poste est obligatoire")
    private String poste;

    @NotNull(message = "Le service est obligatoire")
    private String  service;

    @NotNull(message = "La classification professionnelle est obligatoire")
    private StatutEmployeEnum classificationProfessionnelle;

    @NotNull(message = "La catégorie est obligatoire")
    private CategorieEnum categorie;

    @NotNull(message = "L'échelon est obligatoire")
    private EchelonEnum echelon;

    @NotNull(message = "Le type de contrat est obligatoire")
    private TypeContratEnum typeContratEnum;


    @PastOrPresent (message = "L'acte de naissance ne peut pas etre dans le futur")
    private LocalDate dateNaissance;

    private SexeEnum sexe;

    private BigDecimal salaireBase;
    //nouveau

    //@NotNull(message = " Le solde des jours de congé doit toujours être une valeur connue, même s'il est initialement à zéro")
    @Min(0)
    private BigDecimal soldeJoursConge;

    //@NotNull(message = "Les heures contractuelles doivent toujours etre presente")
    @Min(value = 1, message = "Les heures contractuelles hebdomadaires doivent être supérieures à zéro")
    private BigDecimal heuresContractuellesHebdomadaires;

    @Min(value = 1, message = "Les jours ouvrables contractuels hebdomadaires doivent être supérieurs à zéro")
    @Max(value = 7, message = "Les jours ouvrables ne peuvent pas dépasser 7 par semaine")
    private Integer joursOuvrablesContractuelsHebdomadaires;





}
