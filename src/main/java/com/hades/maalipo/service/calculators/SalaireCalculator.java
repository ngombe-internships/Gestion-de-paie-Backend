package com.hades.maalipo.service.calculators;

import com.hades.maalipo.enum1.CategorieElement;
import com.hades.maalipo.enum1.FormuleCalculType;
import com.hades.maalipo.enum1.TypeAvantageNature;
import com.hades.maalipo.enum1.TypeElementPaie;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.*;
import com.hades.maalipo.repository.ElementPaieRepository;
import com.hades.maalipo.service.EntrepriseParametreRhService;
import com.hades.maalipo.service.EntrepriseService;
import com.hades.maalipo.utils.EntrepriseUtils;
import com.hades.maalipo.utils.MathUtils;
import com.hades.maalipo.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;


@Component
public class SalaireCalculator {

    private final MathUtils mathUtils;
    private final AncienneteService ancienneteService;
    private final ElementPaieRepository elementPaieRepository;
    private final EntrepriseParametreRhService paramService;

    public SalaireCalculator(MathUtils mathUtils, AncienneteService ancienneteService,
                             ElementPaieRepository elementPaieRepository, EntrepriseParametreRhService paramService) {
        this.mathUtils = mathUtils;
        this.ancienneteService = ancienneteService;
        this.elementPaieRepository = elementPaieRepository; // Injection correcte
        this.paramService = paramService;
    }


    //cherche un element de paie existant
    private ElementPaie getOrCreateElementPaie(String designation, TypeElementPaie type, CategorieElement categorie) {
        return elementPaieRepository.findByDesignation(designation)
                .orElseGet(() -> {
                    ElementPaie newElement = ElementPaie.builder()
                            .designation(designation)
                            .type(type)
                            .categorie(categorie)
                            .code(designation.toUpperCase().replaceAll(" ", "_").replaceAll("É", "E")) // Exemple de code

                            .formuleCalcul(TypeElementPaie.GAIN.equals(type) ? FormuleCalculType.MONTANT_FIXE : FormuleCalculType.POURCENTAGE_BASE) // Exemple
                            .build();
                    if ("Prime d'Ancienneté".equals(designation)) {
                        newElement.setFormuleCalcul(FormuleCalculType.POURCENTAGE_BASE);
                    } else if (categorie == CategorieElement.HEURES_SUPPLEMENTAIRES) {
                        //  Pour les heures sup/nuit/férie : NOMBRE_BASE_TAUX
                        newElement.setFormuleCalcul(FormuleCalculType.NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT);
                    } else if (categorie == CategorieElement.SALAIRE_DE_BASE) {
                        newElement.setFormuleCalcul(FormuleCalculType.NOMBRE_BASE_TAUX);
                    } else if (type == TypeElementPaie.GAIN) {
                        // Pour les autres gains (primes fixes)
                        newElement.setFormuleCalcul(FormuleCalculType.MONTANT_FIXE);
                    } else {
                        // Pour les retenues et charges patronales
                        newElement.setFormuleCalcul(FormuleCalculType.POURCENTAGE_BASE);
                    }

                    // Définir les impacts selon le type et la catégorie
                    setElementImpacts(newElement, type, categorie);


                    return elementPaieRepository.save(newElement);
                });

    }
    private void setElementImpacts(ElementPaie element, TypeElementPaie type, CategorieElement categorie) {
        // Par défaut, tout à false
        element.setImpacteSalaireBrut(false);
        element.setImpacteBaseCnps(false);
        element.setImpacteBaseIrpp(false);
        element.setImpacteSalaireBrutImposable(false);
        element.setImpacteBaseCreditFoncier(false);
        element.setImpacteBaseAnciennete(false);
        element.setImpacteNetAPayer(false);

        if (type == TypeElementPaie.GAIN) {
            // TOUS les gains impactent ces éléments de base
            element.setImpacteSalaireBrut(true);
            element.setImpacteNetAPayer(true);
            element.setImpacteSalaireBrutImposable(true);
            element.setImpacteBaseCnps(true);
            element.setImpacteBaseIrpp(true);
            element.setImpacteBaseCreditFoncier(true);

            // CORRECTION : Seule la Prime d'Ancienneté N'IMPACTE PAS la base ancienneté
            if (!"Prime d'Ancienneté".equals(element.getDesignation())) {
                element.setImpacteBaseAnciennete(true);
            }
            // Tous les autres gains (salaire de base, heures sup, autres primes) impactent la base ancienneté

        } else if (type == TypeElementPaie.RETENUE) {
            // Les retenues impactent seulement le net à payer
            element.setImpacteNetAPayer(true);
        }
        // Les charges patronales n'impactent rien par défaut
    }




    public BigDecimal calculerSalaireBase(BulletinPaie bulletinPaie) {
        // 🔧 VÉRIFICATION : Existe-t-il déjà une ligne salaire de base ?
        for (LigneBulletinPaie ligne : bulletinPaie.getLignesPaie()) {
            ElementPaie element = ligne.getElementPaie();
            if (element != null &&
                    element.getCategorie() == CategorieElement.SALAIRE_DE_BASE &&
                    element.getType() == TypeElementPaie.GAIN) {

                System.out.println("✅ Salaire de base déjà présent dans les lignes de paie: " + ligne.getMontantFinal());
                return ligne.getMontantFinal();
            }
        }


        // Si pas trouvé dans les lignes, utiliser la logique existante
        BigDecimal salaireBase = bulletinPaie.getSalaireBaseInitial();

        // Si le salaire de base initial du bulletin n'est pas défini, essayez de le récupérer de l'employé
        if (salaireBase == null || salaireBase.compareTo(BigDecimal.ZERO) <= 0) {
            Employe employe = bulletinPaie.getEmploye();
            if (employe != null && employe.getSalaireBase() != null) {
                salaireBase = employe.getSalaireBase();
                bulletinPaie.setSalaireBaseInitial(salaireBase);
            } else {
                throw new RessourceNotFoundException("Salaire de base non défini pour l'employé " + bulletinPaie.getEmploye().getMatricule());
            }
        }

        // Calculer le nombre d'heures et le taux horaire pour l'affichage
        BigDecimal heuresNormales = bulletinPaie.getHeuresNormal();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        // Si les heures normales ne sont pas définies, calculer à partir du contrat
        if (heuresNormales == null) {
            Employe employe = bulletinPaie.getEmploye();
            if (employe != null && employe.getHeuresContractuellesHebdomadaires() != null) {
                heuresNormales = employe.getHeuresContractuellesHebdomadaires()
                        .multiply(BigDecimal.valueOf(52))
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                bulletinPaie.setHeuresNormal(heuresNormales);
            } else {
                heuresNormales = BigDecimal.valueOf(173.33); // Valeur par défaut (35h * 52 / 12)
            }
        }

        // Si le taux horaire n'est pas défini, le calculer
        if (tauxHoraire == null && heuresNormales.compareTo(BigDecimal.ZERO) > 0) {
            tauxHoraire = salaireBase.divide(heuresNormales, 4, RoundingMode.HALF_UP);
            bulletinPaie.setTauxHoraireInitial(tauxHoraire);
        }

        // 🔧 VÉRIFICATION : S'assurer qu'on n'ajoute pas en double
        boolean salaireBaseExiste = bulletinPaie.getLignesPaie().stream()
                .anyMatch(ligne -> ligne.getElementPaie() != null &&
                        ligne.getElementPaie().getCategorie() == CategorieElement.SALAIRE_DE_BASE);

        if (!salaireBaseExiste) {
            // Ajouter automatiquement le salaire de base comme ligne de paie
            addLignePaieForElement(
                    bulletinPaie,
                    "Salaire de base", // ← Notation cohérente avec le JSON
                    TypeElementPaie.GAIN,
                    CategorieElement.SALAIRE_DE_BASE,
                    heuresNormales,
                    tauxHoraire,
                    salaireBase,
                    salaireBase,
                    salaireBase
            );

            System.out.println("✅ Salaire de base ajouté automatiquement: " + salaireBase);
        } else {
            System.out.println("⚠️ Salaire de base déjà existant, pas d'ajout");
        }

        return salaireBase;
    }

    public void calculerHeuresSupplementaires(BulletinPaie bulletinPaie) {
        BigDecimal totalHeuresSup = bulletinPaie.getHeuresSup();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(bulletinPaie);

        if (totalHeuresSup == null || totalHeuresSup.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal heuresSupRestantes = totalHeuresSup;

        // HS1 : 20% les 8 premières heures
        BigDecimal heuresHS1 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(8));
        if (heuresHS1.compareTo(BigDecimal.ZERO) > 0) {
            String tauxHeureSup1Str = paramService.getParamOrDefault(entrepriseId, "TAUX_HEURE_SUP1", PaieConstants.TAUX_HEURE_SUP1.toString());
            BigDecimal tauxHeureSup1 = new BigDecimal(tauxHeureSup1Str);

            BigDecimal montantHS1 = heuresHS1.multiply(tauxHoraire).multiply(tauxHeureSup1);

            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 20%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS1,
                    tauxHeureSup1, // 0.20
                    tauxHoraire, // base = taux horaire
                    montantHS1,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS1);
        }

        // HS2 : 30% les 8 suivantes
        BigDecimal heuresHS2 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(8));
        if (heuresHS2.compareTo(BigDecimal.ZERO) > 0) {
            String tauxHeureSup2Str = paramService.getParamOrDefault(entrepriseId, "TAUX_HEURE_SUP2", PaieConstants.TAUX_HEURE_SUP2.toString());
            BigDecimal tauxHeureSup2 = new BigDecimal(tauxHeureSup2Str);

            BigDecimal montantHS2 = heuresHS2.multiply(tauxHoraire).multiply(tauxHeureSup2);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 30%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS2,
                    tauxHeureSup2, // 0.30
                    tauxHoraire,
                    montantHS2,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS2);
        }

        String tauxHeureSup3Str = paramService.getParamOrDefault(entrepriseId, "TAUX_HEURE_SUP3", PaieConstants.TAUX_HEURE_SUP3.toString());
        BigDecimal tauxHeureSup3 = new BigDecimal(tauxHeureSup3Str);

        // HS3 : 40% les 4 suivantes
        BigDecimal heuresHS3 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(4));
        if (heuresHS3.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal montantHS3 = heuresHS3.multiply(tauxHoraire).multiply(tauxHeureSup3);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 40%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS3,
                    tauxHeureSup3, // 0.40
                    tauxHoraire,
                    montantHS3,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS3);
        }

        // HS > 20 : tout le reste à 40%
        if (heuresSupRestantes.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal montantHS40Supp = heuresSupRestantes.multiply(tauxHoraire).multiply(tauxHeureSup3);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 40% (supplémentaires)",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresSupRestantes,
                    tauxHeureSup3, // 0.40
                    tauxHoraire,
                    montantHS40Supp,
                    tauxHoraire
            );
        }
    }

    public void calculerHeuresNuit(BulletinPaie bulletinPaie) {
        BigDecimal heuresNuit = bulletinPaie.getHeuresNuit();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(bulletinPaie);


        if (heuresNuit == null || heuresNuit.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal montantCalculHeuresNuit = mathUtils.safeMultiply(heuresNuit, tauxHoraire);


        String tauxHeureSNuitStr = paramService.getParamOrDefault(entrepriseId, "TAUX_HEURE_NUIT", PaieConstants.TAUX_HEURE_NUIT.toString());
        BigDecimal tauxHeureNuit = new BigDecimal(tauxHeureSNuitStr);

        BigDecimal montantHeuresNuit = mathUtils.safeMultiply(montantCalculHeuresNuit, tauxHeureNuit);
        if (montantHeuresNuit.compareTo(BigDecimal.ZERO) > 0) {
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures de Nuit",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresNuit,
                    tauxHeureNuit,
                    mathUtils.safeMultiply(heuresNuit, tauxHoraire),
                    montantHeuresNuit,
                    mathUtils.safeMultiply(heuresNuit,tauxHoraire)
            );
        }
    }


    public void calculerHeuresFerie(BulletinPaie bulletinPaie) {
        BigDecimal heuresFerie = bulletinPaie.getHeuresFerie();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(bulletinPaie);

        if (heuresFerie == null || heuresFerie.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // Utilisation de PaieConstants.TAUX_HEURE_FERIE
        BigDecimal montantCalculHeuresFerie = mathUtils.safeMultiply(heuresFerie, tauxHoraire);

        String tauxHeureSFerieStr = paramService.getParamOrDefault(entrepriseId, "TAUX_HEURE_FERIE", PaieConstants.TAUX_HEURE_FERIE.toString());
        BigDecimal tauxHeureFerie = new BigDecimal(tauxHeureSFerieStr);

        BigDecimal montantHeuresFerie = mathUtils.safeMultiply(montantCalculHeuresFerie,tauxHeureFerie);
        if (montantHeuresFerie.compareTo(BigDecimal.ZERO) > 0) {
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Jours Fériés / Dimanche",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES, // Ou une nouvelle catégorie si vous voulez distinguer
                    heuresFerie,
                    tauxHeureFerie,
                    mathUtils.safeMultiply(heuresFerie, tauxHoraire), // Montant avant majoration
                    montantHeuresFerie,
                    mathUtils.safeMultiply(heuresFerie, tauxHoraire)
            );
        }
    }


    public void calculerPrimeAnciennete(BulletinPaie bulletinPaie) {
        Employe employe = bulletinPaie.getEmploye();
        if (employe == null || employe.getDateEmbauche() == null) {
            return;
        }

        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(bulletinPaie);

        int ancienneteEnAnnees = ancienneteService.calculAncienneteEnAnnees(employe.getDateEmbauche());

        BigDecimal tauxPrimeAnciennete = BigDecimal.ZERO;
        if (ancienneteEnAnnees >= 2) {
            // Taux initial récupéré des paramètres RH ou défaut des constantes
            String tauxInitStr = paramService.getParamOrDefault(entrepriseId, "TAUX_PRIME_ANCIENNETE_INIT", PaieConstants.TAUX_PRIME_ANCIENNETE_INIT.toString());
            tauxPrimeAnciennete = new BigDecimal(tauxInitStr);

            if (ancienneteEnAnnees > 2) {
                // Taux supplémentaire récupéré des paramètres RH ou défaut des constantes
                String tauxSupplStr = paramService.getParamOrDefault(entrepriseId, "TAUX_PRIME_ANCIENNETE_SUPPL", PaieConstants.TAUX_PRIME_ANCIENNETE_SUPPL.toString());
                BigDecimal tauxSuppl = new BigDecimal(tauxSupplStr);

                tauxPrimeAnciennete = tauxPrimeAnciennete.add(BigDecimal.valueOf(ancienneteEnAnnees - 2)
                        .multiply(tauxSuppl));
            }
        }

        if (tauxPrimeAnciennete.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal basePrimeAnciennete = bulletinPaie.getSalaireBaseInitial();
            if (basePrimeAnciennete == null) {
                basePrimeAnciennete = BigDecimal.ZERO;
            }

            BigDecimal montantPrimeAnciennete = mathUtils.safeMultiply(basePrimeAnciennete, tauxPrimeAnciennete);
            if (montantPrimeAnciennete.compareTo(BigDecimal.ZERO) > 0) {
                addLignePaieForElement(
                        bulletinPaie,
                        "Prime d'Ancienneté",
                        TypeElementPaie.GAIN,
                        CategorieElement.PRIME,
                        BigDecimal.valueOf(ancienneteEnAnnees), // Nombre d'années comme "nombre"
                        tauxPrimeAnciennete,
                        basePrimeAnciennete, // La base sur laquelle le taux est appliqué
                        montantPrimeAnciennete,
                        basePrimeAnciennete
                );
            }
        }
    }



    public BigDecimal calculSalaireBrut(BulletinPaie fiche) {
        return fiche.getTotalGains();
    }



    public BigDecimal calculBaseCnps(BulletinPaie fiche) {
        BigDecimal baseCnps = BigDecimal.ZERO;
        System.out.println("=== Calcul Base CNPS ===");

        for (LigneBulletinPaie ligne : fiche.getLignesPaie()) {
            ElementPaie element = ligne.getElementPaie();
            if (element != null && element.isImpacteBaseCnps()) {
                System.out.println("Ajout à base CNPS : " + element.getDesignation() + " = " + ligne.getMontantFinal());
                baseCnps = baseCnps.add(ligne.getMontantFinal());
            }
        }

        Long entrepriseId = EntrepriseUtils.resolveEntrepriseId(fiche);
        String plafondCnpsStr = paramService.getParamOrDefault(entrepriseId, "PLAFOND_CNPS", PaieConstants.PLAFOND_CNPS.toString());
        BigDecimal plafondCnps = new BigDecimal(plafondCnpsStr);


        BigDecimal basePlafonnee = mathUtils.safeMin(baseCnps, plafondCnps);
        System.out.println("Base CNPS avant plafonnement : " + baseCnps);
        System.out.println("Base CNPS après plafonnement : " + basePlafonnee);

        return basePlafonnee;
    }


    public BigDecimal calculSalaireImposable(BulletinPaie fiche) {
        BigDecimal salaireBrutImposable = BigDecimal.ZERO;

        // Ajoute des logs pour débugger
        System.out.println("Calcul salaire imposable :");

        for (LigneBulletinPaie ligne : fiche.getLignesPaie()) {
            ElementPaie element = ligne.getElementPaie();
            if (element != null && element.getType() == TypeElementPaie.GAIN) {
                System.out.println("Élément: " + element.getDesignation()
                        + ", impacteSalaireBrut: " + element.isImpacteSalaireBrut()
                        + ", montant: " + ligne.getMontantFinal());

                if (element.isImpacteSalaireBrut()) {
                    salaireBrutImposable = salaireBrutImposable.add(ligne.getMontantFinal());
                    System.out.println("→ Ajouté au salaire imposable, nouveau total: " + salaireBrutImposable);
                }
            }
        }


        return salaireBrutImposable;
    }

    public BigDecimal calculerTotalAvantageNature(BulletinPaie fiche) {
        Map<TypeAvantageNature, BigDecimal> tauxAvantageNature = Map.of(
                TypeAvantageNature.LOGEMENT, new BigDecimal("0.15"),
                TypeAvantageNature.ELECTRICITE, new BigDecimal("0.04"),
                TypeAvantageNature.EAU, new BigDecimal("0.02"),
                TypeAvantageNature.DOMESTIQUE, new BigDecimal("0.04"),
                TypeAvantageNature.VEHICULE, new BigDecimal("0.10"),
                TypeAvantageNature.NOURRITURE, new BigDecimal("0.10"),
                TypeAvantageNature.TELEPHONE, new BigDecimal("0.05"),
                TypeAvantageNature.CARBURANT, new BigDecimal("0.10"),
                TypeAvantageNature.GARDIENNAGE, new BigDecimal("0.05"),
                TypeAvantageNature.INTERNET, new BigDecimal("0.05")
        );

        BigDecimal salaireBrutTaxable = fiche.getSalaireBrut();
        BigDecimal totalAvantageNature = BigDecimal.ZERO;

        if (fiche.getEmploye().getAvantagesNature() != null) {
            for (EmployeAvantageNature av : fiche.getEmploye().getAvantagesNature()) {
                if (av.isActif()) {
                    BigDecimal taux = tauxAvantageNature.get(av.getTypeAvantage());
                    if (taux != null) {
                        totalAvantageNature = totalAvantageNature.add(salaireBrutTaxable.multiply(taux));
                    }
                }
            }
        }
        return totalAvantageNature.setScale(2, RoundingMode.HALF_UP);
    }



    public BigDecimal calculCoutTotalEmployeur(BulletinPaie fiche) {
        if(fiche.getSalaireNetAPayer() == null || fiche.getTotalChargesPatronales()== null){
            throw new IllegalStateException("Salaire Net à Payer ou Total Charges Patronales non calculés pour le bulletin de paie " + fiche.getId());
        }
        return mathUtils.safeAdd(fiche.getSalaireNetAPayer(), fiche.getTotalChargesPatronales());

    }


    public void addLignePaieForElement(BulletinPaie bulletinPaie, String designation,
                                       TypeElementPaie type, CategorieElement categorie,
                                       BigDecimal nombre, BigDecimal taux, BigDecimal montantCalcul,
                                       BigDecimal montantFinal, BigDecimal baseApplique) {

        // Validation des paramètres
        if (bulletinPaie == null) {
            throw new IllegalArgumentException("BulletinPaie ne peut pas être null");
        }

        if (designation == null || designation.trim().isEmpty()) {
            throw new IllegalArgumentException("Designation ne peut pas être null ou vide");
        }

        ElementPaie elementPaie = getOrCreateElementPaie(designation, type, categorie);

        LigneBulletinPaie ligne = new LigneBulletinPaie();
        ligne.setElementPaie(elementPaie);
        ligne.setNombre(nombre != null ? nombre : BigDecimal.ONE);
        ligne.setTauxApplique(taux != null ? taux : BigDecimal.ZERO);
        ligne.setMontantCalcul(montantCalcul != null ? montantCalcul : BigDecimal.ZERO);
        ligne.setMontantFinal(montantFinal != null ? montantFinal : BigDecimal.ZERO);
        ligne.setBaseApplique(baseApplique != null ? baseApplique : BigDecimal.ZERO);
        ligne.setBulletinPaie(bulletinPaie);


        // Ajoute ici le calcul du tauxAffiche selon le type d’élément
        String tauxAffiche;
        FormuleCalculType formule = elementPaie.getFormuleCalcul();

        if (categorie == CategorieElement.SALAIRE_DE_BASE) {
            tauxAffiche = taux != null ? String.format("%.2f", taux) : "--";
            ligne.setBaseApplique(null); // Pas de base pour le salaire de base

        } else if (formule == FormuleCalculType.MONTANT_FIXE) {
            tauxAffiche = "--";
        } else if (formule == FormuleCalculType.BAREME) {
            tauxAffiche = "BARÈME";
        } else if (categorie == CategorieElement.HEURES_SUPPLEMENTAIRES && taux != null) {
            if (taux.compareTo(BigDecimal.ONE) > 0) {
                // Pour les heures sup : afficher seulement la majoration (ex: 1.25 → +25%)
                BigDecimal majoration = (taux.subtract(BigDecimal.ONE)).multiply(BigDecimal.valueOf(100));
                tauxAffiche = String.format("+%.0f%%", majoration);
            } else if (taux.compareTo(BigDecimal.ONE) == 0) {
                // Taux normal (100%)
                tauxAffiche = "Normal";
            } else {
                // Pour les cas où le taux serait < 1 (peu probable)
                tauxAffiche = String.format("%.2f", taux);
            }
        } else if (formule == FormuleCalculType.TAUX_DEFAUT_X_MONTANT_DEFAUT && taux != null && taux.compareTo(BigDecimal.ZERO) > 0) {
            tauxAffiche = String.format("%.2f%%", taux.multiply(BigDecimal.valueOf(100)));
        } else if (formule == FormuleCalculType.NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT && taux != null && taux.compareTo(BigDecimal.ZERO) > 0) {

            tauxAffiche = String.format("%.2f%%", taux.multiply(BigDecimal.valueOf(100)));
        } else if (formule == FormuleCalculType.POURCENTAGE_BASE && taux != null && taux.compareTo(BigDecimal.ZERO) > 0) {
            tauxAffiche = String.format("%.2f%%", taux.multiply(BigDecimal.valueOf(100)));
        } else if (formule == FormuleCalculType.NOMBRE_BASE_TAUX && taux != null && taux.compareTo(BigDecimal.ZERO) > 0) {
            tauxAffiche = String.format("%.2f", taux); // Taux horaire sans %
        } else {
            tauxAffiche = "--";
        }
        ligne.setTauxAffiche(tauxAffiche);

        // Définir les flags selon le type
        if (type != null) {
            ligne.setEstGain(type == TypeElementPaie.GAIN);
            ligne.setEstRetenue(type == TypeElementPaie.RETENUE);
            ligne.setEstChargePatronale(type == TypeElementPaie.CHARGE_PATRONALE);
            ligne.setType(type);
        }

        bulletinPaie.addLignePaie(ligne);

        System.out.println("✅ Ligne ajoutée - Designation: " + designation +
                ", Type: " + type +
                ", Montant: " + montantFinal);
    }
}