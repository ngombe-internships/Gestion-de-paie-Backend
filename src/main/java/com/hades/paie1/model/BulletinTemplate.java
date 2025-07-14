package com.hades.paie1.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bulletin_template")
@ToString(exclude = {"entreprise", "elementsConfig"})
@EqualsAndHashCode(exclude = {"entreprise", "elementsConfig"})
public class BulletinTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'entreprise à laquelle ce template est associé
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonBackReference("entreprise-templates")
    private Entreprise entreprise;

    @Column(name = "nom_template", nullable = false)
    private String nom; // Ex: "Template Standard", "Template Cadres"

    @Column(name = "is_default", nullable = false)
    private boolean isDefault; // Indique si c'est le template par défaut de l'entreprise

    // --- Configuration des Heures ---


    // --- Liens vers les ElementPaie configurés pour ce template ---
    // Liste des éléments de paie spécifiques activés pour ce template,
    // avec des configurations propres au template (par exemple, valeur par défaut pour ce template)
    @OneToMany(mappedBy = "bulletinTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("template-elements")
    private List<TemplateElementPaieConfig> elementsConfig = new ArrayList<>();

    // Si vous avez besoin de champs spécifiques pour la configuration des heures

}