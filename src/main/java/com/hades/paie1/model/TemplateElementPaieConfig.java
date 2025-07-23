package com.hades.paie1.model; // Ou un nouveau package

import com.fasterxml.jackson.annotation.*;
import com.hades.paie1.enum1.FormuleCalculType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "template_element_paie_config")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class TemplateElementPaieConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    //@JsonBackReference("template-elements")
    private BulletinTemplate bulletinTemplate;// Le template auquel cette config appartient

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_paie_id", nullable = false)
    //@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    //@JsonIdentityReference(alwaysAsId = true)
    private ElementPaie elementPaie; // L'élément de paie générique configuré

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // Est-ce que cet ElementPaie est actif dans ce template ?

    @Column(name = "affichage_ordre")
    private Integer affichageOrdre; // Ordre d'affichage dans le bulletin

//    @Column(name = "valeur_defaut_template", precision = 15, scale = 2)
//    private BigDecimal valeurDefaut; // Une valeur par défaut pour cet élément DANS CE TEMPLATE (ex: Prime Transport = 5000 XAF par défaut pour ce template)

    @Column(name = "taux_defaut_template", precision = 15, scale = 4)
    private BigDecimal tauxDefaut; // Pour les POURCENTAGE_BASE

    @Column(name = "montant_defaut_template", precision = 15, scale = 2)
    private BigDecimal montantDefaut; // Pour les MONTANT_FIXE


    @Enumerated(EnumType.STRING)
    @Column(name = "formule_calcul_override",length = 50)
    private FormuleCalculType formuleCalculOverride; // Permet de surcharger la formule de ElementPaie au niveau du template

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_bulletin_paie_id")
    private LigneBulletinPaie ligneBulletinPaie;


    @Column(name="nombre")
    private BigDecimal nombreDefaut;
    // D'autres configurations spécifiques au template peuvent être ajoutées ici
    // comme 'estModifiableParEmploye', 'estVisibleSurBulletin', etc.


}