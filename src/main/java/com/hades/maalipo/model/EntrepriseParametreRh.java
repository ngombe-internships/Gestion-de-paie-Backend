package com.hades.maalipo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "entreprise_parametres_rh")
public class EntrepriseParametreRh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Column(name = "cle_parametre", nullable = false, length = 100)
    private String cleParametre; // Ex: TAUX_HEURE_SUP1

    @Column(name = "valeur_parametre", nullable = false, length = 100)
    private String valeurParametre; // Stockée en texte, à parser selon le type

    @Column(name = "type_parametre", nullable = false, length = 20)
    private String typeParametre; // DECIMAL, INT, STRING, BOOLEAN

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

}
