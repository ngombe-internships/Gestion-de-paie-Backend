package com.hades.paie1.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "entreprise")
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String nom;

    @Column(nullable = false)
    private String adresseEntreprise;

    @Column(name = "email")
    private String emailEntreprise;

    @Column(name = "telephone")
    private String telephoneEntreprise;


    @Column(unique = true)
    private String numeroSiret;

    @Column(nullable = false)
    private LocalDate dateCreation;

    private String logoUrl;


    @OneToOne(mappedBy =  "entreprise", cascade = CascadeType.ALL, fetch =FetchType.LAZY, optional = true )
    private  User employeurPrincipal;

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List <Employe> employes;



}
