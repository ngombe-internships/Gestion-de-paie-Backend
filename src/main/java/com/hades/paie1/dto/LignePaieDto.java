package com.hades.paie1.dto;


import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeElementPaie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LignePaieDto {
   // private String code;
    private String designation; // Ex: "Salaire de Base", "Prime de Transport", "IRPP"
    private CategorieElement categorie; // Ex: SALAIRE_DE_BASE, PRIME, IMPOT_SUR_REVENU, COTISATION_SALARIALE
    private TypeElementPaie type; // Ex: GAIN, RETENUE, CHARGE_PATRONALE
    private BigDecimal nombre; // Nombre d'unités (ex: heures, jours)
    private BigDecimal tauxApplique; // Taux utilisé (ex: 1.25 pour HS, 0.042 pour CNPS)
    private BigDecimal montantFinal; // Le montant final calculé pour cette ligne
    private String descriptionDetaillee; // Description additionnelle si nécessaire

    private  BigDecimal baseApplique;
    private String tauxSalarial;

 private FormuleCalculType formuleCalcul;


    private BigDecimal baseCalcul;
    private String tauxAffiche;
    private Integer affichageOrdre;

    private BigDecimal tauxPatronal;      // Patronale
    private BigDecimal montantPatronal;
    private boolean isMerged;
    private String tauxPatronalAffiche;
    private boolean isBareme = false;
}