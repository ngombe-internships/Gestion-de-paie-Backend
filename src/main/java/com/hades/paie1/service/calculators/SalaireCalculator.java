package com.hades.paie1.service.calculators;

import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.service.AncienneteService;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Component
public class SalaireCalculator {

    private MathUtils mathUtils;

    private AncienneteService ancienneteService;

    public SalaireCalculator(MathUtils mathUtils , AncienneteService ancienneteService) {
        this.mathUtils = mathUtils;
        this.ancienneteService = ancienneteService;
    }

    public BigDecimal calculSalaireBase(BulletinPaie fiche) {

        return mathUtils.safeMultiply(fiche.getTauxHoraire(), fiche.getHeuresNormal());
    }


    public BigDecimal calculHeureSup1(BulletinPaie fiche) {

        BigDecimal tauxHoraire = fiche.getTauxHoraire();
        BigDecimal heureSup1 = fiche.getHeuresSup1();
        BigDecimal tauxMajore = mathUtils.safeMultiply(tauxHoraire, PaieConstants.TAUX_HEURE_SUP1);

        return mathUtils.safeMultiply(tauxMajore, heureSup1);

    }

    public BigDecimal calculHeureSup2(BulletinPaie fiche) {

        BigDecimal tauxHoraire = fiche.getTauxHoraire();
        BigDecimal heureSup2 = fiche.getHeuresSup2();
        BigDecimal tauxMajore = mathUtils.safeMultiply(tauxHoraire, PaieConstants.TAUX_HEURE_SUP2);

        return mathUtils.safeMultiply(tauxMajore, heureSup2);

    }

    public BigDecimal calculHeureNuit(BulletinPaie fiche) {

        BigDecimal tauxHoraire = fiche.getTauxHoraire();
        BigDecimal heureNuit = fiche.getHeuresNuit();
        BigDecimal tauxMajore = mathUtils.safeMultiply(tauxHoraire, PaieConstants.TAUX_HEURE_NUIT);

        return mathUtils.safeMultiply(tauxMajore, heureNuit);

    }

    public BigDecimal calculHeureFerie(BulletinPaie fiche) {

        BigDecimal tauxHoraire = fiche.getTauxHoraire();
        BigDecimal heureFerie = fiche.getHeuresFerie();
        BigDecimal tauxMajore = mathUtils.safeMultiply(tauxHoraire, PaieConstants.TAUX_HEURE_FERIE);

        return mathUtils.safeMultiply(tauxMajore, heureFerie);

    }

    public BigDecimal calculPrimeAnciennete (BulletinPaie  fiche) {
        if (fiche.getEmploye() == null || fiche.getEmploye().getDateEmbauche() == null) {
            return BigDecimal.ZERO;
        }

        Employe employe = fiche.getEmploye();

        int ancienneteAnnees = ancienneteService.calculAncienneteEnAnnees(employe.getDateEmbauche());

        BigDecimal smc = calculSalaireBase(fiche);

        if(smc == null || smc.compareTo(BigDecimal.ZERO) <=0){
            return BigDecimal.ZERO;
        }

        BigDecimal primeAnciennete = BigDecimal.ZERO;

        if( ancienneteAnnees >=2){
            double tauxAnciennete = 4.0;
            if(ancienneteAnnees >2){
                tauxAnciennete += (ancienneteAnnees -2) *2.0;
            }

            BigDecimal tauxAncienneteDecimal = BigDecimal.valueOf(tauxAnciennete)
                    .divide(new BigDecimal("100"),4,RoundingMode.HALF_UP);

            primeAnciennete = smc.multiply(tauxAncienneteDecimal);
        }
        return primeAnciennete.setScale(2, RoundingMode.HALF_UP);

    }

    public BigDecimal calculTotalPrimes(BulletinPaie fiche) {

        return mathUtils.safeAdd(
                fiche.getPrimeAnciennete(),
                fiche.getPrimeRendement(),
                fiche.getPrimeTransport(),
                fiche.getPrimePonctualite(),
                fiche.getPrimeTechnicite(),
                fiche.getAutrePrimes()
        );
    }

    // calcul du salaire brut sans allocation
    public BigDecimal calculSalaireBrutBase(BulletinPaie fiche) {
        BigDecimal salaireBase = calculSalaireBase(fiche);
        BigDecimal heuresSup1 = calculHeureSup1(fiche);
        BigDecimal heureSup2 = calculHeureSup2(fiche);
        BigDecimal heureNuit = calculHeureNuit(fiche);
        BigDecimal heureFerie = calculHeureFerie(fiche);
        BigDecimal totalPrime = calculTotalPrimes(fiche);




        return mathUtils.safeAdd(salaireBase, heureFerie, heureSup2, heuresSup1, heureNuit, totalPrime);
    }

//    public BigDecimal calculAllocationConge(BulletinPaie fiche) {
//        BigDecimal salaireBrut = calculSalaireBrutBase(fiche);
//        BigDecimal jourConge = fiche.getJourConge();
//
//        if(salaireBrut == null || jourConge == null || jourConge.compareTo(BigDecimal.ZERO) <= 0){
//            return BigDecimal.ZERO;
//        }
//
//        BigDecimal allocationConge = salaireBrut.divide(
//                PaieConstants.JOUR_CONGER,2, RoundingMode.HALF_UP);
//        return  allocationConge.multiply(jourConge);
//    }

    public BigDecimal calculSalaireBrut(BulletinPaie fiche) {
        BigDecimal salaireBrut = calculSalaireBrutBase(fiche);
//        BigDecimal allocationConge = calculAllocationConge(fiche);

        return mathUtils.safeAdd(salaireBrut);
    }


    // Calcul des bases CNPS
    public BigDecimal calculBaseCnps(BulletinPaie fiche) {
        BigDecimal salaireBase = calculSalaireBase(fiche);
        BigDecimal heuresSup = calculHeureSup1(fiche).add(calculHeureSup2(fiche));
        BigDecimal primePonctualite = fiche.getPrimePonctualite();
        BigDecimal primeTechnicite = fiche.getPrimeTechnicite();
        BigDecimal avantageNature = fiche.getAvantageNature();

        BigDecimal baseCnps = mathUtils.safeAdd(salaireBase, primePonctualite, primeTechnicite, avantageNature,heuresSup);

        return mathUtils.safeMin(baseCnps, PaieConstants.PLAFOND_CNPS);
    }

    // Calcul des bases imposables
    public BigDecimal calculSalaireImposable(BulletinPaie fiche) {

        BigDecimal salaireBrut = calculSalaireBrut(fiche);
        BigDecimal avatageNature = fiche.getAvantageNature() != null ? fiche.getAvantageNature() : BigDecimal.ZERO;

        return mathUtils.safeAdd(salaireBrut, avatageNature);
    }


}
