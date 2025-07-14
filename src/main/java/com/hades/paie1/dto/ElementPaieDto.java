package com.hades.paie1.dto;

import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeElementPaie;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ElementPaieDto {
    private Long id;
    private String code;
    //private String intitule;
    //private String memo;
    private TypeElementPaie type;
    private FormuleCalculType formuleCalcul;
    private BigDecimal tauxDefaut;
//    private String uniteBaseCalcul;
    private CategorieElement categorie;
    private String designation;

    private boolean impacteSalaireBrut;
    private boolean impacteBaseCnps;
    private boolean impacteBaseIrpp;
    private boolean impacteSalaireBrutImposable;
    private boolean impacteBaseCreditFoncier;
    private boolean impacteBaseAnciennete;
    private boolean impacteNetAPayer;
}
