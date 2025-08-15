package com.hades.maalipo.model;

import com.hades.maalipo.enum1.TypeConge;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "type_conge_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypeCongeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Min(value = 1, message = "La durée maximale doit être d'au moins 1 jour")
    @Column(name = "duree_maximale_jours")
    private Integer dureeMaximaleJours;  // Override du default enum

    @PositiveOrZero(message = "Le délai de préavis ne peut pas être négatif")
    @Column(name = "delai_preavis_jours")
    private Integer delaiPreavisJours;   // Override du default enum


    @DecimalMin(value = "0.0", inclusive = true, message = "Le pourcentage doit être au moins 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Le pourcentage ne peut pas dépasser 100")
    @Column(name = "pourcentage_remuneration", precision = 5, scale = 2)
    private BigDecimal pourcentageRemuneration = BigDecimal.valueOf(100); // 100% par défaut

    @Column(name = "documents_requis", length = 500)
    private String documentsRequis; // "CERTIFICAT_MEDICAL,ACTE_NAISSANCE"

    @Column(name = "conditions_attribution", length = 1000)
    private String conditionsAttribution; // Conditions spécifiques

    @Column(name = "cumul_autorise")
    private Boolean cumulAutorise = false;

    @Column(name = "documents_obligatoires")
    private Boolean documentsObligatoires = true;


    // Constructeur avec valeurs par défaut de l'enum
    public TypeCongeConfig(Entreprise entreprise, TypeConge typeConge) {
        this.entreprise = entreprise;
        this.typeConge = typeConge;
        this.actif = true;
        this.dureeMaximaleJours = typeConge.getDureeMaximaleJours();
        this.delaiPreavisJours = typeConge.getDelaiPreavisJours();
        this.pourcentageRemuneration = BigDecimal.valueOf(100);
    }
}
