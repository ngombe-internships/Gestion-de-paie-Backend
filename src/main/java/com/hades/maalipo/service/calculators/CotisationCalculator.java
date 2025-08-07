package com.hades.maalipo.service.calculators;

import com.hades.maalipo.enum1.TypeElementPaie;
import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.utils.MathUtils;
import com.hades.maalipo.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component

public class CotisationCalculator {

    private MathUtils mathUtils ;
    private final SalaireCalculator calculator;
    private final ImpotCalculator impotCalculator;

    public  CotisationCalculator (MathUtils mathUtils, SalaireCalculator salaireCalculator, ImpotCalculator impotCalculator){
        this.mathUtils = mathUtils;
        this.calculator = salaireCalculator;
        this.impotCalculator = impotCalculator;
    }


    public BigDecimal calculCnpsVieillesseSalarie(BulletinPaie fiche){
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps, PaieConstants.TAUX_CNPS_VIEILLESSE_SALARIE);
    }

    public BigDecimal calculCreditFoncierSalarie(BulletinPaie fiche){
        BigDecimal salaireBrutImpossable = calculator.calculSalaireImposable(fiche);
        return mathUtils.safeMultiply(salaireBrutImpossable, PaieConstants.TAUX_CREDIT_FONCIER_SALARIE);
    }

    public BigDecimal calculFneSalaire(BulletinPaie fiche){
        BigDecimal salaireBrutImpossable = calculator.calculSalaireImposable(fiche);
        return mathUtils.safeMultiply(salaireBrutImpossable, PaieConstants.TAUX_FNE_SALARIE);
    }

    public BigDecimal calculTotalRetenuesSalaire(BulletinPaie fiche){
       BigDecimal avancesSurSalaires = fiche.getAvancesSurSalaires() != null?
               fiche.getAvancesSurSalaires() : BigDecimal.ZERO;

        return mathUtils.safeAdd(
                impotCalculator.calculIrpp(fiche),
                impotCalculator.calculCac(fiche),
                impotCalculator.calculTaxeCommunal(fiche),
                impotCalculator.calculRedevanceAudioVisuelle(fiche),
                calculCreditFoncierSalarie(fiche),
                calculCnpsVieillesseSalarie(fiche),
                calculFneSalaire(fiche),
                avancesSurSalaires
        );
    }

    // Calcul Des charges patronales
    public BigDecimal calculCnpsVieillesseEmployeur(BulletinPaie fiche){
        BigDecimal baseCnps =  calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps, PaieConstants.TAUX_CNPS_VIEILLESSE_EMPLOYEUR);
    }


    public BigDecimal calculCnpsAllocationsFamiliales(BulletinPaie fiche) {
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps,PaieConstants.TAUX_CNPS_ALLOCATIONS_FAMILIALES);
    }

    public BigDecimal calculCnpsAccidentsTravail(BulletinPaie fiche) {
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps, PaieConstants.TAUX_CNPS_ACCIDENTS_TRAVAIL);
    }

    public BigDecimal calculCreditFoncierPatronal (BulletinPaie  fiche){
        BigDecimal salaireBruteImposable = calculator.calculSalaireImposable(fiche);
        return mathUtils.safeMultiply(salaireBruteImposable,PaieConstants.TAUX_CREDIT_FONCIER_PATRONAL);
    }
    public BigDecimal calculFnePatronal (BulletinPaie  fiche){
        BigDecimal salaireBrute =calculator.calculSalaireImposable(fiche);
        return mathUtils.safeMultiply(salaireBrute,PaieConstants.TAUX_FNE_PATRONAL);
    }

    public BigDecimal calculTotalChargesPatronales(BulletinPaie fiche){
       if (fiche == null || fiche.getLignesPaie() == null){
           return BigDecimal.ZERO;
       }
       return fiche.getLignesPaie().stream()
               .filter(ligne -> ligne.getElementPaie() != null &&
                       ligne.getElementPaie().getType() == TypeElementPaie.CHARGE_PATRONALE)
               .map(ligne -> ligne.getMontantFinal() != null ? ligne.getMontantFinal() : BigDecimal.ZERO)
               .reduce(BigDecimal.ZERO, mathUtils::safeAdd);
    }

    //Calcul total cotisation cnps

    public BigDecimal cotisationCnps (BulletinPaie fiche){
        if (fiche == null || fiche.getLignesPaie() == null) {
            return BigDecimal.ZERO;
        }

        return fiche.getLignesPaie().stream()
                .filter(ligne -> ligne.getElementPaie() != null &&
                        ligne.getElementPaie().getDesignation() != null &&
                        ligne.getElementPaie().getDesignation().toLowerCase().contains("cnps"))
                .map(ligne -> ligne.getMontantFinal() != null ? ligne.getMontantFinal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, mathUtils::safeAdd);

    }





}
