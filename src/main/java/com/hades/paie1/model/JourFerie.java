package com.hades.paie1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "jour_feries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JourFerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_jour_ferie", nullable = false)
    private LocalDate dateFerie;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "est_chome_et_paye", nullable = false)
    private Boolean estChomeEtPaye;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

}
