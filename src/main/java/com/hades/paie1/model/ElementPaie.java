package com.hades.paie1.model;

import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeElementPaie;
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
@Table(name = "element_paie")
public class ElementPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_element", unique = true, nullable = false)
    private String code; // Ex: "AV_LOG"

//    @Column(name = "intitule_element")
//    private String intitule; // Ex: "Avantage en Nature Logement"
//
//    @Column(name = "memo")
//    private String memo; // Ex: "SAL", "PRIME",

    @Enumerated(EnumType.STRING)
    @Column(name = "type_element", nullable = false)
    private TypeElementPaie type;

    @Enumerated(EnumType.STRING)
    @Column(name = "formule_calcul_type", nullable = false)
    private FormuleCalculType formuleCalcul; // MONTANT_FIXE, NOMBRE_BASE_TAUX, POURCENTAGE_BASE, BAREME, AUTRE

    @Column(name = "taux_defaut", precision = 13, scale = 2)
    private BigDecimal tauxDefaut; // Taux par défaut, si applicable

//    @Column(name = "unite_base_calcul")
//    private String uniteBaseCalcul; // Ex: "Salaire Horaires", "Salaire Brut", "Salaire Catégoriel"

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_element", nullable = false)
    private CategorieElement categorie; // SALAIRE_DE_BASE, PRIME, AVANTAGE_EN_NATURE, COTISATION_SALARIALE

    @Column(name = "designation", nullable = false)
    private String designation;

    // Impacts sur les bases de calcul (Boolean)
    @Column(name = "impacte_salaire_brut")
    private boolean impacteSalaireBrut;

    @Column(name = "impacte_base_cnps")
    private boolean impacteBaseCnps;

    @Column(name = "impacte_base_irpp")
    private boolean impacteBaseIrpp;

    @Column(name = "impacte_salaire_brut_imposable")
    private boolean impacteSalaireBrutImposable;

    @Column(name = "impacte_base_credit_foncier")
    private boolean impacteBaseCreditFoncier;

    @Column(name = "impacte_base_anciennete")
    private boolean impacteBaseAnciennete;

    @Column(name = "impacte_net_a_payer")
    private boolean impacteNetAPayer;
}
