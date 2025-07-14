package com.hades.paie1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employe_paie_config")
public class EmployePaieConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe; // L'employé concerné par cette configuration

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_paie_id", nullable = false)
    private ElementPaie elementPaie; // L'élément de paie auquel cette configuration s'applique

    @Column(name = "valeur", precision = 15, scale = 2)
    private BigDecimal valeur; // La valeur spécifique (montant, nombre d'unités, taux) pour cet élément et cet employé

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut; // Date à partir de laquelle cette configuration est valide

    @Column(name = "date_fin")
    private LocalDate dateFin; // Date de fin de validité de cette configuration (peut être null si toujours valide)

    @Column(name = "notes")
    private String notes;

    @Column(name = "taux_personnel", precision = 15, scale = 4)
    private BigDecimal taux; // Pourcentage personnalisé par employé

    @Column(name = "montant_personnel", precision = 15, scale = 2)
    private BigDecimal montant; // Montant personnalisé par employé


}
