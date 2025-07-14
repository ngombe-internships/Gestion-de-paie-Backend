package com.hades.paie1.service.calculators;

import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.service.BulletinPaieService;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ImpotCalculator {

    private MathUtils mathUtils;
    private SalaireCalculator calculator;



    public ImpotCalculator(MathUtils mathUtils, SalaireCalculator salaireCalculator) {
        this.mathUtils = mathUtils;
        this.calculator = salaireCalculator;

    }
    private static final Logger logger = LoggerFactory.getLogger(BulletinPaieService.class);


    //calcul de irpp

    // on calcul dabord pvid pension viellesse Mensuelle
    public BigDecimal  calculPVIDMensuelle (BigDecimal salaireBruteTaxableMensuel){

        BigDecimal salaireCotisable = salaireBruteTaxableMensuel.min(PaieConstants.PLAFOND_CNPS);

        return  salaireCotisable.multiply(new BigDecimal("0.042")).setScale(2, RoundingMode.HALF_UP);
    }

    // on calcul ensuite salaire net categoriel mensuel SNCM
    private BigDecimal calculSNCM(BigDecimal salaireBruteTaxableMensuel , BigDecimal pvidMensuelle) {


        BigDecimal abbattementForfaitaireMensuel = new BigDecimal("500000").divide(new BigDecimal("12"),2, RoundingMode.HALF_UP);

        BigDecimal sncm = (salaireBruteTaxableMensuel.multiply(new BigDecimal("0.70")))
                .subtract(pvidMensuelle)
                .subtract(abbattementForfaitaireMensuel);

        if(sncm.compareTo(BigDecimal.ZERO) < 0){
            return BigDecimal.ZERO;
        }
        return sncm.setScale(2, RoundingMode.HALF_UP);
    }


    // Application des tranches d'IRPP sur le SNCM
    public BigDecimal calculIRPPSurSNCM (BigDecimal sncm){

        BigDecimal irpp = BigDecimal.ZERO;

        //tranche 1 : 0 - 166 667 a 10%
        BigDecimal tranche1Limite = new BigDecimal("166667");
        if(sncm.compareTo(tranche1Limite) > 0){
            irpp = irpp.add(tranche1Limite.multiply(new BigDecimal("0.10")));
        } else {
            irpp = irpp.add(sncm.multiply(new BigDecimal("0.10")));
            return irpp.setScale(2, RoundingMode.HALF_UP);
        }

        //tranche 2 : 166 667 - 250 000 a 15%
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
        BigDecimal montantTranche3 = tranche3Limite.subtract(tranche2Limite); // 166 667
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
    public BigDecimal calculIrpp (BulletinPaie fiche) {

        BigDecimal salaireBrutTaxable = calculator.calculSalaireImposable(fiche);

        if (salaireBrutTaxable.compareTo(new BigDecimal("62000")) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal pvidMensuelle = calculPVIDMensuelle(salaireBrutTaxable);

        BigDecimal sncm = calculSNCM(salaireBrutTaxable, pvidMensuelle);

        BigDecimal irpp = calculIRPPSurSNCM(sncm);
        logger.info("IRPP calculé: {}", irpp);

        return irpp;
    }

    //calcul de CAC
    public  BigDecimal calculCac(BulletinPaie fiche){
        BigDecimal irpp = calculIrpp(fiche);
        System.out.println("IRPP pour CAC: " + irpp);
        System.out.println("TAUX_CAC: " + PaieConstants.TAUX_CAC);
        BigDecimal cac = mathUtils.safeMultiply(irpp, PaieConstants.TAUX_CAC).setScale(2, RoundingMode.HALF_UP);
        System.out.println("CAC calculé: " + cac);
        return cac;
    }

    //Calcul taxe communal

    public BigDecimal calculTaxeCommunal (BulletinPaie fiche){
        BigDecimal salaireBase = calculator.calculerSalaireBase(fiche);

        if(salaireBase.compareTo(PaieConstants.SEUIL_TAXE_COMMUNALE) <=0){
            return  BigDecimal.ZERO;
        }else if(salaireBase.compareTo(new BigDecimal("75000")) <=0 ){
            return new BigDecimal("250");
        }else if(salaireBase.compareTo(new BigDecimal("100000")) <=0 ) {
            return new BigDecimal("500");
        }
        else if(salaireBase.compareTo(new BigDecimal("125000")) <=0 ) {
            return new BigDecimal("750");
        }
        else if(salaireBase.compareTo(new BigDecimal("150000")) <=0 ) {
            return new BigDecimal("1000");
        }
        else if(salaireBase.compareTo(new BigDecimal("200000")) <=0 ) {
            return new BigDecimal("1250");
        }
        else if(salaireBase.compareTo(new BigDecimal("250000")) <=0 ) {
            return new BigDecimal("1500");
        }
        else if(salaireBase.compareTo(new BigDecimal("300000")) <=0 ) {
            return new BigDecimal("2000");
        }
        else if(salaireBase.compareTo(new BigDecimal("500000")) <=0 ) {
            return new BigDecimal("2250");
        }
        else {
            return new BigDecimal("2500");
        }
    }


    // calcul redevance audio visuel
    public BigDecimal calculRedevanceAudioVisuelle(BulletinPaie fiche){
        BigDecimal salaireBrut = calculator.calculSalaireImposable(fiche);

        if(salaireBrut.compareTo(PaieConstants.SEUIL_TAXE_COMMUNALE) <=0){
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
