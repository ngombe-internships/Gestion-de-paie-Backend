package com.hades.maalipo.service.calculators;

import com.hades.maalipo.enum1.TypeElementPaie;
import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.service.EntrepriseParametreRhService;
import com.hades.maalipo.utils.EntrepriseUtils;
import com.hades.maalipo.utils.MathUtils;
import com.hades.maalipo.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CotisationCalculator {

    private final MathUtils mathUtils ;
    private final SalaireCalculator calculator;
    private final ImpotCalculator impotCalculator;
    private final EntrepriseParametreRhService paramService;

    public CotisationCalculator(MathUtils mathUtils, SalaireCalculator salaireCalculator, ImpotCalculator impotCalculator, EntrepriseParametreRhService paramService){
        this.mathUtils = mathUtils;
        this.calculator = salaireCalculator;
        this.impotCalculator = impotCalculator;
        this.paramService = paramService;
    }

    public BigDecimal calculCnpsVieillesseSalarie(BulletinPaie fiche){
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCnpsVieillesseSalarieStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CNPS_VIEILLESSE_SALARIE", PaieConstants.TAUX_CNPS_VIEILLESSE_SALARIE.toString());
        BigDecimal tauxCnpsVieillesseSalarie = new BigDecimal(tauxCnpsVieillesseSalarieStr);
        return mathUtils.safeMultiply(baseCnps, tauxCnpsVieillesseSalarie);
    }

    public BigDecimal calculCreditFoncierSalarie(BulletinPaie fiche){
        BigDecimal salaireBrutImposable = calculator.calculSalaireImposable(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCreditFoncierSalarieStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CREDIT_FONCIER_SALARIE", PaieConstants.TAUX_CREDIT_FONCIER_SALARIE.toString());
        BigDecimal tauxCreditFoncierSalarie = new BigDecimal(tauxCreditFoncierSalarieStr);
        return mathUtils.safeMultiply(salaireBrutImposable, tauxCreditFoncierSalarie);
    }

    public BigDecimal calculFneSalaire(BulletinPaie fiche){
        BigDecimal salaireBrutImposable = calculator.calculSalaireImposable(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxFneSalarieStr = paramService.getParamOrDefault(entrepriseId, "TAUX_FNE_SALARIE", PaieConstants.TAUX_FNE_SALARIE.toString());
        BigDecimal tauxFneSalarie = new BigDecimal(tauxFneSalarieStr);
        return mathUtils.safeMultiply(salaireBrutImposable, tauxFneSalarie);
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

    // Charges patronales
    public BigDecimal calculCnpsVieillesseEmployeur(BulletinPaie fiche){
        BigDecimal baseCnps =  calculator.calculBaseCnps(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCnpsVieillesseEmployeurStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CNPS_VIEILLESSE_EMPLOYEUR", PaieConstants.TAUX_CNPS_VIEILLESSE_EMPLOYEUR.toString());
        BigDecimal tauxCnpsVieillesseEmployeur = new BigDecimal(tauxCnpsVieillesseEmployeurStr);
        return mathUtils.safeMultiply(baseCnps, tauxCnpsVieillesseEmployeur);
    }

    public BigDecimal calculCnpsAllocationsFamiliales(BulletinPaie fiche) {
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCnpsAllocationsFamilialesStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CNPS_ALLOCATIONS_FAMILIALES", PaieConstants.TAUX_CNPS_ALLOCATIONS_FAMILIALES.toString());
        BigDecimal tauxCnpsAllocationsFamiliales = new BigDecimal(tauxCnpsAllocationsFamilialesStr);
        return mathUtils.safeMultiply(baseCnps, tauxCnpsAllocationsFamiliales);
    }

    public BigDecimal calculCnpsAccidentsTravail(BulletinPaie fiche) {
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCnpsAccidentsTravailStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CNPS_ACCIDENTS_TRAVAIL", PaieConstants.TAUX_CNPS_ACCIDENTS_TRAVAIL.toString());
        BigDecimal tauxCnpsAccidentsTravail = new BigDecimal(tauxCnpsAccidentsTravailStr);
        return mathUtils.safeMultiply(baseCnps, tauxCnpsAccidentsTravail);
    }

    public BigDecimal calculCreditFoncierPatronal(BulletinPaie fiche){
        BigDecimal salaireBrutImposable = calculator.calculSalaireImposable(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCreditFoncierPatronalStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CREDIT_FONCIER_PATRONAL", PaieConstants.TAUX_CREDIT_FONCIER_PATRONAL.toString());
        BigDecimal tauxCreditFoncierPatronal = new BigDecimal(tauxCreditFoncierPatronalStr);
        return mathUtils.safeMultiply(salaireBrutImposable, tauxCreditFoncierPatronal);
    }

    public BigDecimal calculFnePatronal(BulletinPaie fiche){
        BigDecimal salaireBrutImposable = calculator.calculSalaireImposable(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxFnePatronalStr = paramService.getParamOrDefault(entrepriseId, "TAUX_FNE_PATRONAL", PaieConstants.TAUX_FNE_PATRONAL.toString());
        BigDecimal tauxFnePatronal = new BigDecimal(tauxFnePatronalStr);
        return mathUtils.safeMultiply(salaireBrutImposable, tauxFnePatronal);
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

    // Total cotisation CNPS
    public BigDecimal cotisationCnps(BulletinPaie fiche){
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