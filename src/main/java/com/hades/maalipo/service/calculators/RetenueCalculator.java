package com.hades.maalipo.service.calculators;

import com.hades.maalipo.enum1.CategorieElement;
import com.hades.maalipo.enum1.FormuleCalculType;
import com.hades.maalipo.enum1.TypeElementPaie;
import com.hades.maalipo.model.*;
import com.hades.maalipo.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class RetenueCalculator {

    private final CotisationCalculator cotisationCalculator;
    private final ImpotCalculator impotCalculator;

    public RetenueCalculator(CotisationCalculator cotisationCalculator, ImpotCalculator impotCalculator) {
        this.cotisationCalculator = cotisationCalculator;
        this.impotCalculator = impotCalculator;
    }

    //Calcule le montant d'une retenue pour une ligne de bulletin de paie.

   public void calculerMontantRetenue(
            LigneBulletinPaie ligne,
            ElementPaie element,
            FormuleCalculType formule,
            BigDecimal valeur,
            BulletinPaie fiche,
            Optional<EmployePaieConfig> employeConfig,
            TemplateElementPaieConfig config) {

        String code = element.getCode().toUpperCase();
        String designation = element.getDesignation() != null ? element.getDesignation().toUpperCase() : "";

        if (element.getCategorie() == CategorieElement.SALAIRE_DE_BASE ||
                "SALAIRE DE BASE".equals(designation) ||
                "SALAIRE_DE_BASE".equals(code)) {
            System.out.println("⚠️ SKIP: Salaire de base déjà géré par SalaireCalculator - " + designation);
            return;
        }

        BigDecimal montant = BigDecimal.ZERO;
        BigDecimal baseUtilisee = null;
        BigDecimal tauxApplique = valeur;
        String tauxAffiche = null;

        BigDecimal montantSpecifique = calculerCotisationSpecifique(code, designation, fiche);
        if (montantSpecifique != null) {
            System.out.println("Montant calculé via CotisationCalculator pour " + code + ": " + montantSpecifique);
            montant = montantSpecifique;
            baseUtilisee = determinerBaseCalcul(element, fiche);
            tauxApplique = obtenirTauxDepuisConstants(code, designation, element);

            if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
                baseUtilisee = impotCalculator.calculIrpp(fiche);
                tauxApplique = PaieConstants.TAUX_CAC;
            } else {
                baseUtilisee = determinerBaseCalcul(element, fiche);
                tauxApplique = obtenirTauxDepuisConstants(code, designation, element);
            }

            if (tauxApplique == null || tauxApplique.compareTo(BigDecimal.ZERO) == 0) {
                if (valeur != null && valeur.compareTo(BigDecimal.ZERO) > 0) {
                    tauxApplique = valeur;
                } else {
                    tauxApplique = calculerTauxEffectif(montant, baseUtilisee);
                }
            }
        } else {
            switch (formule) {
                case MONTANT_FIXE:
                    montant = employeConfig.isPresent() && employeConfig.get().getMontant() != null ?
                            employeConfig.get().getMontant() :
                            (config.getMontantDefaut() != null ? config.getMontantDefaut() :
                                    (element.getTauxDefaut() != null ? element.getTauxDefaut() : BigDecimal.ZERO));
                    baseUtilisee = null;
                    tauxApplique = null;
                    tauxAffiche = null;
                    break;

                case POURCENTAGE_BASE:
                    baseUtilisee = determinerBaseCalcul(element, fiche);
                    tauxApplique = employeConfig.isPresent() && employeConfig.get().getTaux() != null ?
                            employeConfig.get().getTaux() :
                            (config.getTauxDefaut() != null ? config.getTauxDefaut() :
                                    (element.getTauxDefaut() != null ? element.getTauxDefaut() : BigDecimal.ZERO));
                    BigDecimal tauxConstant = obtenirTauxDepuisConstants(code, designation, element);
                    if (tauxConstant != null && tauxConstant.compareTo(BigDecimal.ZERO) > 0) {
                        tauxApplique = tauxConstant;
                    }
                    montant = baseUtilisee.multiply(tauxApplique);
                    if (tauxApplique != null) {
                        tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
                    }
                    break;
                //nbre * taux
                case NOMBRE_BASE_TAUX:
                    BigDecimal nombre = BigDecimal.ONE;
                    tauxApplique = employeConfig.isPresent() && employeConfig.get().getTaux() != null ?
                            employeConfig.get().getTaux() :
                            (config.getTauxDefaut() != null ? config.getTauxDefaut() :
                                    (element.getTauxDefaut() != null ? element.getTauxDefaut() : BigDecimal.ZERO));
                    montant = nombre.multiply(tauxApplique);
                    tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
                    break;

                case TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal tauxDefaut = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefaut = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getMontantDefaut();
                    montant = tauxDefaut.multiply(montantDefaut);
                    tauxApplique = tauxDefaut;
                    tauxAffiche = String.format("%.2f %%", tauxDefaut.multiply(BigDecimal.valueOf(100)));
                    baseUtilisee = montantDefaut;
                    ligne.setNombre(config.getNombreDefaut() != null ? config.getNombreDefaut() : BigDecimal.ONE);
                    ligne.setBaseApplique(baseUtilisee);
                    break;

                case NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal nombreX = config.getNombreDefaut() != null ? config.getNombreDefaut() : BigDecimal.ONE;
                    BigDecimal tauxDefautX = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefautX = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getMontantDefaut();
                    montant = nombreX.multiply(tauxDefautX).multiply(montantDefautX);
                    tauxApplique = tauxDefautX;
                    tauxAffiche = String.format("%.2f %%", tauxDefautX.multiply(BigDecimal.valueOf(100)));
                    baseUtilisee = montantDefautX;
                    ligne.setNombre(nombreX);
                    ligne.setBaseApplique(baseUtilisee);
                    break;

                case BAREME:
                    baseUtilisee = determinerBaseCalcul(element, fiche);
                    montant = calculerMontantBareme(code, fiche);
                    tauxApplique = null;
                    break;

                default:
                    montant = BigDecimal.ZERO;
                    tauxApplique = BigDecimal.ZERO;
            }
        }

        TypeElementPaie typeElement = determinerTypeElement(element, code, designation);


       if (element.getCategorie() == CategorieElement.SALAIRE_DE_BASE ||
               "SALAIRE DE BASE".equals(designation) ||
               "SALAIRE_DE_BASE".equals(code)) {
           tauxAffiche = tauxApplique != null ? String.format("%.2f", tauxApplique) : "--";
       } else if (formule == FormuleCalculType.BAREME) {
           tauxAffiche = "BARÈME";
       } else if (formule == FormuleCalculType.POURCENTAGE_BASE && tauxApplique != null && tauxApplique.compareTo(BigDecimal.ZERO) > 0) {
           tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
       } else if (formule == FormuleCalculType.MONTANT_FIXE) {
           tauxAffiche = "--";
       } else if (formule == FormuleCalculType.TAUX_DEFAUT_X_MONTANT_DEFAUT && tauxApplique != null && tauxApplique.compareTo(BigDecimal.ZERO) > 0) {
           // FIX: Ajouter l'assignation manquante
           tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
       } else if (formule == FormuleCalculType.NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT && tauxApplique != null && tauxApplique.compareTo(BigDecimal.ZERO) > 0) {
           // FIX: Ajouter l'assignation manquante
           tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
       } else {
           tauxAffiche = "--";
       }

       System.out.println("DEBUG - Element: " + element.getDesignation());
       System.out.println("DEBUG - Code: " + code);
       System.out.println("DEBUG - Formule: " + formule);
       System.out.println("DEBUG - TauxApplique: " + tauxApplique);
       System.out.println("DEBUG - TauxAffiche calculé: " + tauxAffiche);
       System.out.println("DEBUG - Montant: " + montant);
       System.out.println("DEBUG ----------------------------------------");

        ligne.setTauxAffiche(tauxAffiche);
        ligne.setElementPaie(element);
       ligne.setNombre(ligne.getNombre() != null ? ligne.getNombre() : BigDecimal.ONE);
       ligne.setTauxApplique(tauxApplique);
        ligne.setMontantCalcul(montant);
        ligne.setMontantFinal(montant);
        ligne.setBaseApplique(baseUtilisee);
        ligne.setType(typeElement);
        ligne.setTauxAffiche(tauxAffiche);
        ligne.setEstGain(typeElement == TypeElementPaie.GAIN);
        ligne.setEstRetenue(typeElement == TypeElementPaie.RETENUE);
        ligne.setEstChargePatronale(typeElement == TypeElementPaie.CHARGE_PATRONALE);

        if (formule == FormuleCalculType.BAREME) {
            ligne.setBareme(true);
        }

        ligne.setFormuleCalcul(formule);

        System.out.println("Ligne créée - Code: " + code + ", Montant: " + montant +
                ", Taux: " + tauxApplique + ", Base: " + baseUtilisee + ", Type: " + typeElement +
                ", Formule: " + formule + ",TauxAffiche " + tauxAffiche);
    }

    // Méthodes privées utilitaires
    private TypeElementPaie determinerTypeElement(ElementPaie element, String code, String designation) {
        if (element.getType() != null) {
            return element.getType();
        }
        if (code.contains("SALAIRE_BASE") || code.contains("SALAIRE_BRUT") ||
                designation.contains("SALAIRE DE BASE") || designation.contains("SALAIRE BRUT") ||
                element.getCategorie() == CategorieElement.SALAIRE_DE_BASE) {
            return TypeElementPaie.GAIN;
        }
        if (code.contains("EMPLOYEUR") || code.contains("PATRONAL") ||
                designation.contains("EMPLOYEUR") || designation.contains("PATRONAL")) {
            return TypeElementPaie.CHARGE_PATRONALE;
        }
        if (element.getCategorie() == CategorieElement.COTISATION_PATRONALE) {
            return TypeElementPaie.CHARGE_PATRONALE;
        }
        return TypeElementPaie.RETENUE;
    }

    private BigDecimal obtenirTauxDepuisConstants(String code, String designation, ElementPaie element) {
        // CNPS Vieillesse
        if ((code.contains("CNPS_VIEILLESSE") || (designation.contains("CNPS") && designation.contains("VIEILLESSE")))) {
            if (code.contains("EMPLOYEUR") || designation.contains("EMPLOYEUR") || code.contains("PATRONAL") ||
                    element.getCategorie() == CategorieElement.COTISATION_PATRONALE) {
                return PaieConstants.TAUX_CNPS_VIEILLESSE_EMPLOYEUR;
            } else {
                return PaieConstants.TAUX_CNPS_VIEILLESSE_SALARIE;
            }
        }
        if (code.contains("CNPS_ALLOCATIONS_FAMILIALES") ||
                code.contains("ALLOCATION_FAMILIALE_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ALLOCATION"))) {
            return PaieConstants.TAUX_CNPS_ALLOCATIONS_FAMILIALES;
        }
        if (code.contains("CNPS_ACCIDENTS_TRAVAIL") ||
                code.contains("ACCIDENT_TRAVAIL_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ACCIDENT"))) {
            return PaieConstants.TAUX_CNPS_ACCIDENTS_TRAVAIL;
        }
        if (code.contains("CREDIT_FONCIER")) {
            if (code.contains("SALARIE") || code.contains("SALARIAL") || designation.contains("SALARI")) {
                return PaieConstants.TAUX_CREDIT_FONCIER_SALARIE;
            } else if (code.contains("PATRONAL") || designation.contains("PATRONAL")) {
                return PaieConstants.TAUX_CREDIT_FONCIER_PATRONAL;
            }
        }
        if (code.contains("FNE") || (designation.contains("FONDS NATIONAL") && designation.contains("EMPLOI"))) {
            if (code.contains("SALARIE") || code.contains("SALARIAL") || designation.contains("SALARI")) {
                return PaieConstants.TAUX_FNE_SALARIE;
            } else if (code.contains("PATRONAL") || designation.contains("PATRONAL")) {
                return PaieConstants.TAUX_FNE_PATRONAL;
            }
        }
        if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
            return PaieConstants.TAUX_CAC;
        }
        if ("IRPP".equalsIgnoreCase(code) || designation.contains("IRPP") ||
                code.contains("TAXE_COMMUNALE") || designation.contains("TAXE COMMUNALE") ||
                code.contains("REDEVANCE_AUDIOVISUELLE") || designation.contains("REDEVANCE AUDIOVISUELLE")) {
            return null;
        }
        return null;
    }

    private BigDecimal calculerCotisationSpecifique(String code, String designation, BulletinPaie fiche) {
        if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
            return impotCalculator.calculCac(fiche);
        }
        if ("200".equals(code) || "IRPP".equalsIgnoreCase(code) || designation.contains("IRPP")) {
            return impotCalculator.calculIrpp(fiche);
        }
        if (code.contains("TAXE_COMMUNALE") || designation.contains("TAXE COMMUNALE")) {
            return impotCalculator.calculTaxeCommunal(fiche);
        }
        if (code.contains("REDEVANCE_AUDIOVISUELLE") || designation.contains("REDEVANCE AUDIOVISUELLE")) {
            return impotCalculator.calculRedevanceAudioVisuelle(fiche);
        }
        if (code.contains("CNPS_VIEILLESSE") || (designation.contains("CNPS") && designation.contains("VIEILLESSE"))) {
            if (code.contains("EMPLOYEUR") || designation.contains("EMPLOYEUR") || code.contains("PATRONAL")) {
                return cotisationCalculator.calculCnpsVieillesseEmployeur(fiche);
            } else if (code.contains("SALARIE") || designation.contains("SALARIE") || code.contains("SALARIAL")) {
                return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
            } else {
                ElementPaie element = fiche.getLignesPaie().stream()
                        .filter(l -> l.getElementPaie().getCode().equals(code))
                        .findFirst()
                        .map(LigneBulletinPaie::getElementPaie)
                        .orElse(null);
                if (element != null) {
                    if (element.getCategorie() == CategorieElement.COTISATION_PATRONALE) {
                        return cotisationCalculator.calculCnpsVieillesseEmployeur(fiche);
                    } else {
                        return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
                    }
                }
                return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
            }
        }
        if (code.contains("CNPS_ALLOCATIONS_FAMILIALES") ||
                code.contains("ALLOCATION_FAMILIALE_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ALLOCATION"))) {
            return cotisationCalculator.calculCnpsAllocationsFamiliales(fiche);
        }
        if (code.contains("CNPS_ACCIDENTS_TRAVAIL") ||
                code.contains("ACCIDENT_TRAVAIL_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ACCIDENT"))) {
            return cotisationCalculator.calculCnpsAccidentsTravail(fiche);
        }
        if (code.contains("CREDIT_FONCIER_SALARIE") ||
                code.contains("CREDIT_FONCIER_SALARIAL") ||
                (designation.contains("CRÉDIT FONCIER") && designation.contains("SALARI"))) {
            return cotisationCalculator.calculCreditFoncierSalarie(fiche);
        }
        if (code.contains("CREDIT_FONCIER_PATRONAL") ||
                (designation.contains("CRÉDIT FONCIER") && designation.contains("PATRONAL"))) {
            return cotisationCalculator.calculCreditFoncierPatronal(fiche);
        }
        if (code.contains("FNE_SALARIE") ||
                code.contains("FONDS_NATIONAL_EMPLOI") ||
                (designation.contains("FONDS NATIONAL") && designation.contains("EMPLOI"))) {
            return cotisationCalculator.calculFneSalaire(fiche);
        }
        if (code.contains("FNE_PATRONAL") ||
                (designation.contains("FONDS NATIONAL") && designation.contains("PATRONAL"))) {
            return cotisationCalculator.calculFnePatronal(fiche);
        }
        if (code.equals("TOTAL_CNPS") || designation.contains("TOTAL") && designation.contains("CNPS")) {
            return cotisationCalculator.cotisationCnps(fiche);
        }
        if (code.equals("TOTAL_CHARGES_PATRONALES") || designation.contains("TOTAL") && designation.contains("CHARGES PATRONALES")) {
            return cotisationCalculator.calculTotalChargesPatronales(fiche);
        }
        if (code.equals("TOTAL_RETENUES_SALARIE") || designation.contains("TOTAL") && designation.contains("RETENUES")) {
            return cotisationCalculator.calculTotalRetenuesSalaire(fiche);
        }
        return null;
    }

    private BigDecimal determinerBaseCalcul(ElementPaie element, BulletinPaie fiche) {
        String code = element.getCode().toUpperCase();
        String designation = element.getDesignation() != null ? element.getDesignation().toUpperCase() : "";

        if ("CAC".equalsIgnoreCase(code)) {
            BigDecimal irppMontant = impotCalculator.calculIrpp(fiche);
            return irppMontant;
        }
        if (designation.contains("TAXE COMMUNALE") || code.contains("TAXE_COMMUNALE")) {
            return fiche.getSalaireBaseInitial() != null ? fiche.getSalaireBaseInitial() : BigDecimal.ZERO;
        }
        if (designation.contains("CNPS") || code.contains("CNPS")) {
            return fiche.getBaseCnps() != null ? fiche.getBaseCnps() : BigDecimal.ZERO;
        }
        if (designation.contains("CRÉDIT FONCIER") || code.contains("CREDIT_FONCIER") ||
                designation.contains("FONDS NATIONAL") || code.contains("FNE") ||
                designation.contains("TAXE COMMUNALE") || code.contains("TAXE_COMMUNALE") ||
                designation.contains("REDEVANCE AUDIOVISUELLE") || code.contains("REDEVANCE_AUDIOVISUELLE")) {
            return fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
        }
        if ("IRPP".equalsIgnoreCase(code)) {
            return fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
        }
        return element.isImpacteBaseCnps() && fiche.getBaseCnps() != null
                ? fiche.getBaseCnps()
                : fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
    }

    private BigDecimal calculerTauxEffectif(BigDecimal montant, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montant.divide(base, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerMontantBareme(String code, BulletinPaie fiche) {
        if ("IRPP".equalsIgnoreCase(code)) {
            return impotCalculator.calculIrpp(fiche);
        } else if ("TAXE_COMMUNALE".equalsIgnoreCase(code)) {
            return impotCalculator.calculTaxeCommunal(fiche);
        } else if ("REDEVANCE_AUDIOVISUELLE".equalsIgnoreCase(code)) {
            return impotCalculator.calculRedevanceAudioVisuelle(fiche);
        }
        return BigDecimal.ZERO;
    }
}