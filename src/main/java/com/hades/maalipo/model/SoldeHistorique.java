package com.hades.maalipo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "solde_historique")
@Data
public class SoldeHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employe_id")
    private Employe employe;

    private LocalDate dateCalcul;

    private BigDecimal soldeAcquis;

    private BigDecimal soldePris;
}