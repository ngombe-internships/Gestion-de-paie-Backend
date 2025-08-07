package com.hades.maalipo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.hades.maalipo.enum1.FormuleCalculType;
import com.hades.maalipo.enum1.TypeElementPaie;
import com.hades.maalipo.enum1.TypeLigne;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ligne_bulletin_paie")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class LigneBulletinPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulletin_paie_id", nullable = false)
    @JsonBackReference("bulletin-lignes")
    private BulletinPaie bulletinPaie; // Le bulletin de paie auquel cette ligne appartient

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_paie_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private ElementPaie elementPaie; // L'élément de paie du catalogue auquel cette ligne se réfère

    @Enumerated(EnumType.STRING)
    @Column(name = "type_ligne")
    private TypeLigne typeLigne;




    private String tauxAffiche;

    @Column(name = "montant_calcul", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantCalcul;

    @Column(name = "montant_final", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantFinal; // Le montant final calculé pour cet élément sur ce bulletin

    @Column(name = "nombre", precision = 15, scale = 4)
    private BigDecimal nombre; // Valeur "nombre" utilisée pour le calcul (ex: heures sup, jours travaillés)

    @Column(name = "base_calcul", precision = 15, scale = 2)
    private BigDecimal baseCalcul; // Valeur de la base de calcul utilisée (ex: salaire brut, base CNPS)

    @Column(name = "taux_applique", precision = 15, scale = 4)
    private BigDecimal tauxApplique;

    @Column(name = "description_detaillee", length = 500)
    private String descriptionDetaillee;

    @Column(name = "base_applique")
    private BigDecimal baseApplique;

    private TypeElementPaie type;

    @Column(name = "est_gain")
    private boolean estGain;

    @Column(name = "est_retenue")
    private boolean estRetenue;

    @Column(name = "est_charge_patronale")
    private boolean estChargePatronale;





    @OneToOne(mappedBy = "ligneBulletinPaie", fetch = FetchType.LAZY)
    private TemplateElementPaieConfig templateElementPaieConfig;

    @Enumerated(EnumType.STRING)
    private FormuleCalculType formuleCalcul;

    @Column(name = "is_bareme", nullable = false)
    private boolean isBareme = false;


    private BigDecimal tauxPatronal;      // Patronale
    private BigDecimal montantPatronal;
    private boolean isMerged;
    private String tauxPatronalAffiche;
}