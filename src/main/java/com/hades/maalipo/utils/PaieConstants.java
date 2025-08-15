package com.hades.maalipo.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaieConstants {

    public PaieConstants() {}

    // Constante Heure
    public static final BigDecimal TAUX_HEURE_SUP1 = new BigDecimal("1.20");
    public static final BigDecimal TAUX_HEURE_SUP2 = new BigDecimal("1.30");
    public static final BigDecimal TAUX_HEURE_SUP3 = new BigDecimal("1.40");
    public static final BigDecimal TAUX_HEURE_NUIT = new BigDecimal("1.50");
    public static final BigDecimal TAUX_HEURE_FERIE = new BigDecimal("2.00");

    //plafond et seuils corriges
    public  static final BigDecimal PLAFOND_CNPS = new BigDecimal("750000");
    public  static final BigDecimal SEUIL_TAXE_COMMUNALE = new BigDecimal("63323");


    // Taux de cotisations sociales - Part salarié CORRIGÉS
    public static final BigDecimal TAUX_CNPS_VIEILLESSE_SALARIE = new BigDecimal("0.042"); // 2.8%
    public static final BigDecimal TAUX_CREDIT_FONCIER_SALARIE = new BigDecimal("0.01"); // 1%
    public static final BigDecimal TAUX_FNE_SALARIE = new BigDecimal("0.01"); // 1%

    // Taux de cotisations sociales - Part patronale CORRIGÉS
    public static final BigDecimal TAUX_CNPS_VIEILLESSE_EMPLOYEUR = new BigDecimal("0.042"); // 4.2%
    public static final BigDecimal TAUX_CNPS_ALLOCATIONS_FAMILIALES = new BigDecimal("0.07"); // 7%
    public static final BigDecimal TAUX_CNPS_ACCIDENTS_TRAVAIL = new BigDecimal("0.025"); // 2.5%
    public static final BigDecimal TAUX_CREDIT_FONCIER_PATRONAL = new BigDecimal("0.015"); // 1.5%
    public static final BigDecimal TAUX_FNE_PATRONAL = new BigDecimal("0.01"); // 1%

    // Taux CAC (Centimes Additionnels Communaux)
    public static final BigDecimal TAUX_CAC = new BigDecimal("0.10"); // 10%


    // Division du nombre jour
    public static final BigDecimal JOUR_CONGER = new BigDecimal("24");


    //jour conge base 18 jours
    public static final BigDecimal JOURCONGESBASE = new BigDecimal("18.0");


    public static final BigDecimal TAUX_PRIME_ANCIENNETE_INIT= new BigDecimal("0.04"); //4%
    public static final BigDecimal TAUX_PRIME_ANCIENNETE_SUPPL = new BigDecimal("0.02");


}

