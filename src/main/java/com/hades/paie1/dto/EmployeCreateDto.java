package com.hades.paie1.dto;


import com.hades.paie1.enum1.*;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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


    @Column(name="sexe")
    private CiviliteEnum sexe;



}
