package com.hades.paie1.service.calculators;

import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeAvantageNature;
import com.hades.paie1.enum1.TypeElementPaie;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.*;
import com.hades.paie1.repository.BulletinPaieRepo;
import com.hades.paie1.repository.ElementPaieRepository;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;


@Component
public class SalaireCalculator {

    private final MathUtils mathUtils;
    private final AncienneteService ancienneteService;
    private final ElementPaieRepository elementPaieRepository;

    public SalaireCalculator(MathUtils mathUtils, AncienneteService ancienneteService,
                             ElementPaieRepository elementPaieRepository) {
        this.mathUtils = mathUtils;
        this.ancienneteService = ancienneteService;

        this.elementPaieRepository = elementPaieRepository; // Injection correcte
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
                        newElement.setFormuleCalcul(FormuleCalculType.POURCENTAGE_BASE); // Spécifique pour la prime d'ancienneté
                    } else {
                       newElement.setFormuleCalcul(TypeElementPaie.GAIN.equals(type) ? FormuleCalculType.MONTANT_FIXE : FormuleCalculType.POURCENTAGE_BASE);
                    }
                    newElement.setCode(designation.toUpperCase().replace(" ", "_"));


                    return elementPaieRepository.save(newElement);
                });

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

        if (totalHeuresSup == null || totalHeuresSup.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal heuresSupRestantes = totalHeuresSup;

        // HS1 : 20% les 8 premières heures
        BigDecimal heuresHS1 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(8));
        if (heuresHS1.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantHS1 = heuresHS1.multiply(tauxHoraire).multiply(PaieConstants.TAUX_HEURE_SUP1);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 20%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS1,
                    PaieConstants.TAUX_HEURE_SUP1, // 0.20
                    tauxHoraire, // base = taux horaire
                    montantHS1,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS1);
        }

        // HS2 : 30% les 8 suivantes
        BigDecimal heuresHS2 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(8));
        if (heuresHS2.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantHS2 = heuresHS2.multiply(tauxHoraire).multiply(PaieConstants.TAUX_HEURE_SUP2);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 30%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS2,
                    PaieConstants.TAUX_HEURE_SUP2, // 0.30
                    tauxHoraire,
                    montantHS2,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS2);
        }

        // HS3 : 40% les 4 suivantes
        BigDecimal heuresHS3 = mathUtils.safeMin(heuresSupRestantes, BigDecimal.valueOf(4));
        if (heuresHS3.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantHS3 = heuresHS3.multiply(tauxHoraire).multiply(PaieConstants.TAUX_HEURE_SUP3);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 40%",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresHS3,
                    PaieConstants.TAUX_HEURE_SUP3, // 0.40
                    tauxHoraire,
                    montantHS3,
                    tauxHoraire
            );
            heuresSupRestantes = heuresSupRestantes.subtract(heuresHS3);
        }

        // HS > 20 : tout le reste à 40%
        if (heuresSupRestantes.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantHS40Supp = heuresSupRestantes.multiply(tauxHoraire).multiply(PaieConstants.TAUX_HEURE_SUP3);
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Supplémentaires 40% (supplémentaires)",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresSupRestantes,
                    PaieConstants.TAUX_HEURE_SUP3, // 0.40
                    tauxHoraire,
                    montantHS40Supp,
                    tauxHoraire
            );
        }
    }

    public void calculerHeuresNuit(BulletinPaie bulletinPaie) {
        BigDecimal heuresNuit = bulletinPaie.getHeuresNuit();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        if (heuresNuit == null || heuresNuit.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal montantCalculHeuresNuit = mathUtils.safeMultiply(heuresNuit, tauxHoraire);
        BigDecimal montantHeuresNuit = mathUtils.safeMultiply(montantCalculHeuresNuit, PaieConstants.TAUX_HEURE_NUIT);
        if (montantHeuresNuit.compareTo(BigDecimal.ZERO) > 0) {
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures de Nuit",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES,
                    heuresNuit,
                    PaieConstants.TAUX_HEURE_NUIT,
                    mathUtils.safeMultiply(heuresNuit, tauxHoraire),
                    montantHeuresNuit,
                    mathUtils.safeMultiply(heuresNuit,tauxHoraire)
            );
        }
    }


    public void calculerHeuresFerie(BulletinPaie bulletinPaie) {
        BigDecimal heuresFerie = bulletinPaie.getHeuresFerie();
        BigDecimal tauxHoraire = bulletinPaie.getTauxHoraireInitial();

        if (heuresFerie == null || heuresFerie.compareTo(BigDecimal.ZERO) <= 0 ||
                tauxHoraire == null || tauxHoraire.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // Utilisation de PaieConstants.TAUX_HEURE_FERIE
        BigDecimal montantCalculHeuresFerie = mathUtils.safeMultiply(heuresFerie, tauxHoraire);
        BigDecimal montantHeuresFerie = mathUtils.safeMultiply(montantCalculHeuresFerie, PaieConstants.TAUX_HEURE_FERIE);
        if (montantHeuresFerie.compareTo(BigDecimal.ZERO) > 0) {
            addLignePaieForElement(
                    bulletinPaie,
                    "Heures Jours Fériés / Dimanche",
                    TypeElementPaie.GAIN,
                    CategorieElement.HEURES_SUPPLEMENTAIRES, // Ou une nouvelle catégorie si vous voulez distinguer
                    heuresFerie,
                    PaieConstants.TAUX_HEURE_FERIE,
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

        int ancienneteEnAnnees = ancienneteService.calculAncienneteEnAnnees(employe.getDateEmbauche());

        BigDecimal tauxPrimeAnciennete = BigDecimal.ZERO;
        if (ancienneteEnAnnees >= 2) {
            // Taux initial de 4% après 2 ans.
            tauxPrimeAnciennete = PaieConstants.TAUX_PRIME_ANCIENNETE_INIT;
            if (ancienneteEnAnnees > 2) {

                tauxPrimeAnciennete = tauxPrimeAnciennete.add(BigDecimal.valueOf(ancienneteEnAnnees - 2)
                        .multiply(PaieConstants.TAUX_PRIME_ANCIENNETE_SUPPL));
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

        BigDecimal basePlafonnee = mathUtils.safeMin(baseCnps, PaieConstants.PLAFOND_CNPS);
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

        if (categorie == CategorieElement.SALAIRE_DE_BASE) {
            tauxAffiche = taux != null ? String.format("%.2f", taux) : "--";
            ligne.setBaseApplique(null);
        } else if (elementPaie.getFormuleCalcul() == FormuleCalculType.BAREME) {
            tauxAffiche = "BARÈME";
        } else if (elementPaie.getFormuleCalcul() == FormuleCalculType.POURCENTAGE_BASE && ligne.getTauxApplique() != null) {
            tauxAffiche = String.format("%.2f %%", ligne.getTauxApplique().multiply(BigDecimal.valueOf(100)));
        } else if (elementPaie.getFormuleCalcul() == FormuleCalculType.MONTANT_FIXE) {
            tauxAffiche = "--";
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