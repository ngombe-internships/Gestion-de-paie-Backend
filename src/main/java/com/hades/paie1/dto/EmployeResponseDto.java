package com.hades.paie1.dto;

import com.hades.paie1.enum1.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeResponseDto {

    private Long id; // Inclure l'ID pour les réponses

    private String matricule;
    private String nom;
    private String prenom;
    private String numeroCnps;
    private String niu;
    private String telephone;
    private String email;
    private String adresse;
    private LocalDate dateEmbauche;
    private  LocalDate dateNaissance;
    private String poste;
    private String service;
    private StatutEmployeEnum classificationProfessionnelle;
    private CategorieEnum categorie;
    private EchelonEnum echelon;
    private TypeContratEnum typeContratEnum;
    private CiviliteEnum civilite;


}
