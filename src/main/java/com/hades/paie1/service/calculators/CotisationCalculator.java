package com.hades.paie1.service.calculators;

import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CotisationCalculator {

    private MathUtils mathUtils ;
    private SalaireCalculator calculator;
    private ImpotCalculator impotCalculator;

    public  CotisationCalculator (MathUtils mathUtils, SalaireCalculator salaireCalculator, ImpotCalculator impotCalculator){
        this.mathUtils = mathUtils;
        this.calculator = salaireCalculator;
        this.impotCalculator = impotCalculator;
    }

    // calcul  des cotisation sociales

    public BigDecimal calculCnpsVieillesseSalarie(BulletinPaie fiche){
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps, PaieConstants.TAUX_CNPS_VIEILLESSE_EMPLOYEUR);
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
        return mathUtils.safeAdd(
                impotCalculator.calculIrpp(fiche),
                impotCalculator.calculCac(fiche),
                impotCalculator.calculTaxeCommunal(fiche),
                impotCalculator.calculRedevanceAudioVisuelle(fiche),
                calculCreditFoncierSalarie(fiche),
                calculCnpsVieillesseEmployeur(fiche)
        );
    }

    // Calcul Des charges patronales
    public BigDecimal calculCnpsVieillesseEmployeur(BulletinPaie fiche){
        BigDecimal baseCnps =  calculator.calculBaseCnps(fiche);
        return mathUtils.safeMultiply(baseCnps, PaieConstants.TAUX_CNPS_VIEILLESSE_SALARIE);
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
        return mathUtils.safeAdd(
                calculCnpsAccidentsTravail(fiche),
                calculCnpsVieillesseEmployeur(fiche),
                calculCreditFoncierPatronal(fiche),
                calculFnePatronal(fiche),
                calculCnpsAllocationsFamiliales(fiche)
        );
    }

    //Calcul total cotisation cnps

    public BigDecimal cotisationCnps (BulletinPaie fiche){
        return  mathUtils.safeAdd(
                calculCnpsVieillesseEmployeur(fiche),
                calculCnpsVieillesseSalarie(fiche),
                calculCnpsAllocationsFamiliales(fiche),
                calculCnpsAccidentsTravail(fiche)
        );
    }
}
