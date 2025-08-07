package com.hades.maalipo.service.calculators;

import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.service.BulletinPaieService;
import com.hades.maalipo.service.EntrepriseParametreRhService;
import com.hades.maalipo.utils.EntrepriseUtils;
import com.hades.maalipo.utils.MathUtils;
import com.hades.maalipo.utils.PaieConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ImpotCalculator {

    private final MathUtils mathUtils;
    private final SalaireCalculator calculator;
    private final EntrepriseParametreRhService paramService;

    private static final Logger logger = LoggerFactory.getLogger(BulletinPaieService.class);

    public ImpotCalculator(
            MathUtils mathUtils,
            SalaireCalculator salaireCalculator,
            EntrepriseParametreRhService paramService
    ) {
        this.mathUtils = mathUtils;
        this.calculator = salaireCalculator;
        this.paramService = paramService;
    }

    // Calcul Pension Vieillesse Mensuelle (PVID) avec gestion dynamique du plafond CNPS
    public BigDecimal calculPVIDMensuelle(BulletinPaie fiche, BigDecimal salaireBruteTaxableMensuel){
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String plafondCnpsStr = paramService.getParamOrDefault(entrepriseId, "PLAFOND_CNPS", PaieConstants.PLAFOND_CNPS.toString());
        BigDecimal plafondCnps = new BigDecimal(plafondCnpsStr);

        BigDecimal salaireCotisable = salaireBruteTaxableMensuel.min(plafondCnps);
        return salaireCotisable.multiply(new BigDecimal("0.042")).setScale(2, RoundingMode.HALF_UP);
    }

    // Calcul du salaire net catégoriel mensuel (SNCM)
    private BigDecimal calculSNCM(BulletinPaie fiche, BigDecimal salaireBruteTaxableMensuel, BigDecimal pvidMensuelle) {
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String abbattementMensuelStr = paramService.getParamOrDefault(entrepriseId, "ABBATTEMENT_FORFAITAIRE_MENSUEL", "500000");
        BigDecimal abbattementForfaitaireMensuel = new BigDecimal(abbattementMensuelStr).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

        BigDecimal sncm = (salaireBruteTaxableMensuel.multiply(new BigDecimal("0.70")))
                .subtract(pvidMensuelle)
                .subtract(abbattementForfaitaireMensuel);

        if(sncm.compareTo(BigDecimal.ZERO) < 0){
            return BigDecimal.ZERO;
        }
        return sncm.setScale(2, RoundingMode.HALF_UP);
    }

    // Application des tranches d'IRPP sur le SNCM
    public BigDecimal calculIRPPSurSNCM(BigDecimal sncm){
        BigDecimal irpp = BigDecimal.ZERO;

        // Tranche 1 : 0 - 166 667 à 10%
        BigDecimal tranche1Limite = new BigDecimal("166667");
        if(sncm.compareTo(tranche1Limite) > 0){
            irpp = irpp.add(tranche1Limite.multiply(new BigDecimal("0.10")));
        } else {
            irpp = irpp.add(sncm.multiply(new BigDecimal("0.10")));
            return irpp.setScale(2, RoundingMode.HALF_UP);
        }

        // Tranche 2 : 166 667 - 250 000 à 15%
        BigDecimal tranche2Limite = new BigDecimal("250000");
        BigDecimal montantTranche2 = tranche2Limite.subtract(tranche1Limite);
        if (sncm.compareTo(tranche2Limite) > 0){
            irpp = irpp.add(montantTranche2.multiply(new BigDecimal("0.15")));
        } else {
            irpp = irpp.add(sncm.subtract(tranche1Limite).multiply(new BigDecimal("0.15")));
            return irpp.setScale(2, RoundingMode.HALF_UP);
        }

        // Tranche 3 : 250 000 - 416 667 à 25%
        BigDecimal tranche3Limite = new BigDecimal("416667");
        BigDecimal montantTranche3 = tranche3Limite.subtract(tranche2Limite);
        if (sncm.compareTo(tranche3Limite) > 0) {
            irpp = irpp.add(montantTranche3.multiply(new BigDecimal("0.25")));
        } else {
            irpp = irpp.add(sncm.subtract(tranche2Limite).multiply(new BigDecimal("0.25")));
            return irpp.setScale(2, RoundingMode.HALF_UP);
        }

        // Tranche 4 : Au-delà de 416 667 à 35%
        BigDecimal surplus = sncm.subtract(tranche3Limite);
        if (surplus.compareTo(BigDecimal.ZERO) > 0) {
            irpp = irpp.add(surplus.multiply(new BigDecimal("0.35")));
        }

        return irpp.setScale(2, RoundingMode.HALF_UP);
    }

    // Calcul de l'IRPP selon le barème camerounais officiel avec SNCM
    public BigDecimal calculIrpp(BulletinPaie fiche) {
        BigDecimal salaireBrutTaxable = calculator.calculSalaireImposable(fiche);

        // Seuil imposable dynamique
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String seuilImposableStr = paramService.getParamOrDefault(entrepriseId, "SEUIL_IRPP", "62000");
        BigDecimal seuilImposable = new BigDecimal(seuilImposableStr);

        if (salaireBrutTaxable.compareTo(seuilImposable) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal pvidMensuelle = calculPVIDMensuelle(fiche, salaireBrutTaxable);
        BigDecimal sncm = calculSNCM(fiche, salaireBrutTaxable, pvidMensuelle);
        BigDecimal irpp = calculIRPPSurSNCM(sncm);
        logger.info("IRPP calculé: {}", irpp);

        return irpp;
    }

    // Calcul de CAC dynamique
    public BigDecimal calculCac(BulletinPaie fiche){
        BigDecimal irpp = calculIrpp(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String tauxCacStr = paramService.getParamOrDefault(entrepriseId, "TAUX_CAC", PaieConstants.TAUX_CAC.toString());
        BigDecimal tauxCac = new BigDecimal(tauxCacStr);

        BigDecimal cac = mathUtils.safeMultiply(irpp, tauxCac).setScale(2, RoundingMode.HALF_UP);
        return cac;
    }

    // Calcul taxe communale dynamique
    public BigDecimal calculTaxeCommunal(BulletinPaie fiche){
        BigDecimal salaireBase = calculator.calculerSalaireBase(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String seuilTaxeCommunaleStr = paramService.getParamOrDefault(entrepriseId, "SEUIL_TAXE_COMMUNALE", PaieConstants.SEUIL_TAXE_COMMUNALE.toString());
        BigDecimal seuilTaxeCommunale = new BigDecimal(seuilTaxeCommunaleStr);

        if(salaireBase.compareTo(seuilTaxeCommunale) <= 0){
            return BigDecimal.ZERO;
        }else if(salaireBase.compareTo(new BigDecimal("75000")) <= 0 ){
            return new BigDecimal("250");
        }else if(salaireBase.compareTo(new BigDecimal("100000")) <= 0 ) {
            return new BigDecimal("500");
        }
        else if(salaireBase.compareTo(new BigDecimal("125000")) <= 0 ) {
            return new BigDecimal("750");
        }
        else if(salaireBase.compareTo(new BigDecimal("150000")) <= 0 ) {
            return new BigDecimal("1000");
        }
        else if(salaireBase.compareTo(new BigDecimal("200000")) <= 0 ) {
            return new BigDecimal("1250");
        }
        else if(salaireBase.compareTo(new BigDecimal("250000")) <= 0 ) {
            return new BigDecimal("1500");
        }
        else if(salaireBase.compareTo(new BigDecimal("300000")) <= 0 ) {
            return new BigDecimal("2000");
        }
        else if(salaireBase.compareTo(new BigDecimal("500000")) <= 0 ) {
            return new BigDecimal("2250");
        }
        else {
            return new BigDecimal("2500");
        }
    }

    // Calcul redevance audio visuelle dynamique
    public BigDecimal calculRedevanceAudioVisuelle(BulletinPaie fiche){
        BigDecimal salaireBrut = calculator.calculSalaireImposable(fiche);
        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String seuilTaxeCommunaleStr = paramService.getParamOrDefault(entrepriseId, "SEUIL_TAXE_COMMUNALE", PaieConstants.SEUIL_TAXE_COMMUNALE.toString());
        BigDecimal seuilTaxeCommunale = new BigDecimal(seuilTaxeCommunaleStr);

        if(salaireBrut.compareTo(seuilTaxeCommunale) <= 0){
            return  BigDecimal.ZERO;
        }else if (salaireBrut.compareTo(new BigDecimal("100000")) <= 0){
            return new BigDecimal("750");
        }
        else if (salaireBrut.compareTo(new BigDecimal("200000")) <= 0){
            return new BigDecimal("1950");
        }
        else if (salaireBrut.compareTo(new BigDecimal("300000")) <= 0){
            return new BigDecimal("3250");
        }
        else if (salaireBrut.compareTo(new BigDecimal("400000")) <= 0){
            return new BigDecimal("4550");
        }
        else if (salaireBrut.compareTo(new BigDecimal("500000")) <= 0){
            return new BigDecimal("5850");
        }
        else if (salaireBrut.compareTo(new BigDecimal("600000")) <= 0){
            return new BigDecimal("7150");
        }
        else if (salaireBrut.compareTo(new BigDecimal("700000")) <= 0){
            return new BigDecimal("8450");
        }
        else if (salaireBrut.compareTo(new BigDecimal("800000")) <= 0){
            return new BigDecimal("9750");
        }
        else if (salaireBrut.compareTo(new BigDecimal("900000")) <= 0){
            return new BigDecimal("11050");
        }
        else if (salaireBrut.compareTo(new BigDecimal("1000000")) <= 0){
            return new BigDecimal("12350");
        }else{
            return new BigDecimal("13000");
        }
    }
}