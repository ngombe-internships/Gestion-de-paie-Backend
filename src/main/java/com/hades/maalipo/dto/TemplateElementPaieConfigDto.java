package com.hades.maalipo.dto;

import com.hades.maalipo.enum1.FormuleCalculType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TemplateElementPaieConfigDto {
    private Long id;
    private Long elementPaieId;;
    private boolean isActive;

    private FormuleCalculType formuleCalculOverride;
    private Integer affichageOrdre;

    private BigDecimal tauxDefaut; // Pour les POURCENTAGE_BASE
    private BigDecimal montantDefaut; // Pour les MONTANT_FIXE

    private BigDecimal nombreDefaut;


}
