package com.hades.paie1.service;

import com.hades.paie1.dto.LignePaieDto;
import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeElementPaie;
import com.hades.paie1.model.LigneBulletinPaie;
import com.hades.paie1.model.TemplateElementPaieConfig;
import com.hades.paie1.repository.TemplateElementPaieConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollDisplayService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollDisplayService.class);

    private final TemplateElementPaieConfigRepository templateElementPaieConfigRepository;

    public PayrollDisplayService(TemplateElementPaieConfigRepository templateElementPaieConfigRepository) {
        this.templateElementPaieConfigRepository = templateElementPaieConfigRepository;
    }

    private LignePaieDto convertLigneBulletinPaieToDto(LigneBulletinPaie ligne) {
        Integer affichageOrdre = null;
        String tauxAffiche = null;

        // Correction ici !
        if (ligne.getElementPaie() != null) {
            FormuleCalculType formule = ligne.getElementPaie().getFormuleCalcul();
            if (formule == FormuleCalculType.BAREME) {
                tauxAffiche = "BARÈME";
            } else if (formule == FormuleCalculType.MONTANT_FIXE) {
                tauxAffiche = ""; // Toujours "-" pour montant fixe
            } else if (ligne.getTauxApplique() != null && ligne.getTauxApplique().compareTo(BigDecimal.ZERO) != 0) {
                tauxAffiche = ligne.getTauxApplique().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toString() + " %";
            } else {
                tauxAffiche = "";
            }
        } else if (ligne.getTauxApplique() != null && ligne.getTauxApplique().compareTo(BigDecimal.ZERO) != 0) {
            tauxAffiche = ligne.getTauxApplique().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toString() + " %";
        } else {
            tauxAffiche = "-";
        }

        if (ligne.getTemplateElementPaieConfig() != null) {
            affichageOrdre = ligne.getTemplateElementPaieConfig().getAffichageOrdre();
        }
        return LignePaieDto.builder()
                .affichageOrdre(affichageOrdre)
                .designation(ligne.getElementPaie() != null ? ligne.getElementPaie().getDesignation() : "N/A")
                .categorie(ligne.getElementPaie() != null ? ligne.getElementPaie().getCategorie() : null)
                .type(ligne.getElementPaie() != null ? ligne.getElementPaie().getType() : null)
                .nombre(ligne.getNombre() != null && ligne.getNombre().compareTo(BigDecimal.ONE) == 0 ? null : ligne.getNombre())
                .tauxApplique(ligne.getTauxApplique())
                .montantFinal(ligne.getMontantFinal())
                .descriptionDetaillee(ligne.getDescriptionDetaillee())
                .tauxAffiche(tauxAffiche)
                .baseApplique(ligne.getBaseApplique())
                .formuleCalcul(ligne.getFormuleCalcul())
                .build();
    }

    private String formatTauxPourcentage(BigDecimal taux) {
        if (taux == null) return "-";

        // 🔧 CORRECTION : Logique simplifiée pour le formatage des taux
        if (taux.compareTo(BigDecimal.ONE) > 0) {
            // Taux déjà en pourcentage (ex: 5.5 = 5.5%)
            return taux.setScale(2, RoundingMode.HALF_UP).toString() + " %";
        } else {
            // Taux en décimal (ex: 0.055 = 5.5%)
            return taux.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toString() + " %";
        }
    }

    public List<LignePaieDto> prepareLignesPaieForDisplay(List<LigneBulletinPaie> lignesBulletinPaie) {
        // 1. Convertir toutes les LigneBulletinPaie en LignePaieDto initiales
        List<LignePaieDto> lignesPaieDto = lignesBulletinPaie.stream()
                .map(this::convertLigneBulletinPaieToDto)
                .collect(Collectors.toList());

        logger.info("=== DÉBUT DU PROCESSUS DE FUSION ===");
        logger.info("Lignes DTO après conversion initiale (avant fusion et tri):");
        lignesPaieDto.forEach(ligne -> logger.info("  - Designation: {}, Ordre: {}, Type: {}, Categorie: {}, Base: {}",
                ligne.getDesignation(), ligne.getAffichageOrdre(), ligne.getType(), ligne.getCategorie(), ligne.getBaseApplique()));

        List<LignePaieDto> fusionnees = new ArrayList<>();
        Set<Integer> dejaPris = new HashSet<>();

        for (int i = 0; i < lignesPaieDto.size(); i++) {
            LignePaieDto ligne = lignesPaieDto.get(i);

            // Si la ligne a déjà été prise, on passe
            if (dejaPris.contains(i)) {
                continue;
            }

            // 🔧 CONDITION ÉLARGIE : Détecter tous les éléments de cotisation fusionnables
            boolean isCotisationFusionnable = isCotisationFusionnable(ligne);

            if (isCotisationFusionnable) {
                logger.info("🔍 Tentative de fusion pour: {} (Type: {}, Catégorie: {})",
                        ligne.getDesignation(), ligne.getType(), ligne.getCategorie());

                // Chercher le jumeau (part patronale/salariale)
                LignePaieDto twin = null;
                int twinIndex = -1;

                for (int j = i + 1; j < lignesPaieDto.size(); j++) {
                    LignePaieDto potentialTwin = lignesPaieDto.get(j);

                    if (dejaPris.contains(j)) {
                        continue;
                    }

                    // Vérifier si c'est le jumeau
                    if (areCompatibleCotisationLines(ligne, potentialTwin)) {
                        twin = potentialTwin;
                        twinIndex = j;
                        logger.info("✅ Jumeau trouvé: {} pour {}", twin.getDesignation(), ligne.getDesignation());
                        break;
                    }
                }

                if (twin != null) {
                    // Créer la ligne fusionnée
                    LignePaieDto fusion = createMergedCotisationLine(ligne, twin);
                    fusionnees.add(fusion);
                    dejaPris.add(i);
                    dejaPris.add(twinIndex);
                    logger.info("✅ Fusion réussie pour: {}", fusion.getDesignation());
                } else {
                    logger.warn("❌ Aucun jumeau trouvé pour: {}", ligne.getDesignation());
                    fusionnees.add(ligne);
                }
            } else {
                // Si la ligne n'est pas fusionnable, l'ajouter telle quelle
                fusionnees.add(ligne);
            }
        }

        logger.info("=== RÉSULTAT DE LA FUSION ===");
        logger.info("Lignes DTO après fusion (avant tri final):");
        fusionnees.forEach(ligne -> logger.info("  - Designation: {}, Ordre: {}, EstFusionnee: {}, MontantSalarial: {}, MontantPatronal: {}",
                ligne.getDesignation(), ligne.getAffichageOrdre(), ligne.isMerged(), ligne.getMontantFinal(), ligne.getMontantPatronal()));

        // Trier par ordre d'affichage
        fusionnees.sort(Comparator.comparing(LignePaieDto::getAffichageOrdre,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return fusionnees;
    }

    /**
     * 🔧 MÉTHODE ÉLARGIE : Détermine si une ligne correspond à une cotisation fusionnable
     */
    private boolean isCotisationFusionnable(LignePaieDto ligne) {
        if (ligne.getDesignation() == null) {
            return false;
        }

        String designation = ligne.getDesignation().toUpperCase();

        // 🔧 ÉLARGI : Vérifier plusieurs types de cotisations
        return (designation.contains("CNPS") &&
                (designation.contains("VIEILLESSE") ||
                        designation.contains("PENSION") ||
                        designation.contains("VIELLESSE"))) ||
                (designation.contains("CNPS") && designation.contains("ACCIDENT")) ||
                (designation.contains("CNPS") && designation.contains("FAMILLE")) ||
                (designation.contains("CNPS") && designation.contains("MALADIE"));
    }

    /**
     * 🔧 MÉTHODE AMÉLIORÉE : Vérifie si deux lignes de cotisation peuvent être fusionnées
     */
    private boolean areCompatibleCotisationLines(LignePaieDto ligne1, LignePaieDto ligne2) {
        // Les deux doivent être des cotisations fusionnables
        if (!isCotisationFusionnable(ligne1) || !isCotisationFusionnable(ligne2)) {
            return false;
        }

        // 🔧 AMÉLIORATION : Vérifier que c'est le même type de cotisation
        if (!isSameCotisationType(ligne1, ligne2)) {
            return false;
        }

        // Même base de calcul (avec tolérance pour les arrondis)
        boolean sameBase = areBasesEqual(ligne1.getBaseApplique(), ligne2.getBaseApplique());

        // Une doit être salariale et l'autre patronale
        boolean isComplementary = isComplementaryTypes(ligne1, ligne2);

        logger.info("🔍 Vérification compatibilité: {} vs {} - SameBase: {}, Complementary: {}",
                ligne1.getDesignation(), ligne2.getDesignation(), sameBase, isComplementary);

        return sameBase && isComplementary;
    }

    /**
     * 🔧 NOUVELLE MÉTHODE : Vérifie si deux lignes concernent le même type de cotisation
     */
    private boolean isSameCotisationType(LignePaieDto ligne1, LignePaieDto ligne2) {
        String des1 = ligne1.getDesignation().toUpperCase();
        String des2 = ligne2.getDesignation().toUpperCase();

        // Vérifier les mots-clés principaux
        String[] keywords = {"VIEILLESSE", "PENSION", "VIELLESSE", "ACCIDENT", "FAMILLE", "MALADIE"};

        for (String keyword : keywords) {
            if (des1.contains(keyword) && des2.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 🔧 NOUVELLE MÉTHODE : Vérifie l'égalité des bases avec tolérance
     */
    private boolean areBasesEqual(BigDecimal base1, BigDecimal base2) {
        if (base1 == null && base2 == null) return true;
        if (base1 == null || base2 == null) return false;

        // Tolérance pour les arrondis
        BigDecimal difference = base1.subtract(base2).abs();
        return difference.compareTo(BigDecimal.valueOf(0.01)) <= 0;
    }

    /**
     * 🔧 MÉTHODE AMÉLIORÉE : Vérifie si deux lignes sont complémentaires (salariale/patronale)
     */
    private boolean isComplementaryTypes(LignePaieDto ligne1, LignePaieDto ligne2) {
        // Méthode 1: Vérifier par type
        if (ligne1.getType() != null && ligne2.getType() != null) {
            boolean method1 = (ligne1.getType() == TypeElementPaie.RETENUE && ligne2.getType() == TypeElementPaie.CHARGE_PATRONALE) ||
                    (ligne1.getType() == TypeElementPaie.CHARGE_PATRONALE && ligne2.getType() == TypeElementPaie.RETENUE);
            if (method1) {
                logger.info("✅ Complémentarité détectée par TYPE: {} vs {}", ligne1.getType(), ligne2.getType());
                return true;
            }
        }

        // Méthode 2: Vérifier par catégorie
        if (ligne1.getCategorie() != null && ligne2.getCategorie() != null) {
            boolean method2 = (ligne1.getCategorie() == CategorieElement.COTISATION_SALARIALE &&
                    ligne2.getCategorie() == CategorieElement.COTISATION_PATRONALE) ||
                    (ligne1.getCategorie() == CategorieElement.COTISATION_PATRONALE &&
                            ligne2.getCategorie() == CategorieElement.COTISATION_SALARIALE);
            if (method2) {
                logger.info("✅ Complémentarité détectée par CATÉGORIE: {} vs {}", ligne1.getCategorie(), ligne2.getCategorie());
                return true;
            }
        }

        // Méthode 3: Vérifier par désignation (fallback)
        String des1 = ligne1.getDesignation().toUpperCase();
        String des2 = ligne2.getDesignation().toUpperCase();

        boolean ligne1IsPatronale = des1.contains("EMPLOYEUR") || des1.contains("PATRONAL") || des1.contains("PATRON");
        boolean ligne2IsPatronale = des2.contains("EMPLOYEUR") || des2.contains("PATRONAL") || des2.contains("PATRON");

        boolean method3 = ligne1IsPatronale != ligne2IsPatronale;
        if (method3) {
            logger.info("✅ Complémentarité détectée par DÉSIGNATION: {} vs {}", des1, des2);
        }

        return method3;
    }

    /**
     * 🔧 MÉTHODE AMÉLIORÉE : Crée une ligne fusionnée pour les cotisations
     */
    private LignePaieDto createMergedCotisationLine(LignePaieDto ligne1, LignePaieDto ligne2) {
        // Déterminer quelle ligne est salariale et quelle est patronale
        LignePaieDto salariale = getSalarialeFromPair(ligne1, ligne2);
        LignePaieDto patronale = getPatronaleFromPair(ligne1, ligne2);

        // Utiliser la désignation la plus générique
        String designation = getGenericDesignation(ligne1.getDesignation(), ligne2.getDesignation());

        logger.info("🔧 Création ligne fusionnée: {} | Salariale: {} | Patronale: {}",
                designation, salariale.getMontantFinal(), patronale.getMontantFinal());

        return LignePaieDto.builder()
                .designation(designation)
                .baseApplique(salariale.getBaseApplique())
                .categorie(CategorieElement.COTISATION_SALARIALE) // Catégorie principale
                .type(TypeElementPaie.RETENUE) // Type principal
                .isMerged(true)
                .affichageOrdre(getValidAffichageOrdre(ligne1, ligne2))

                // Partie salariale
                .tauxApplique(salariale.getTauxApplique())
                .montantFinal(salariale.getMontantFinal())
                .tauxAffiche(salariale.getTauxAffiche())

                // Partie patronale
                .tauxPatronal(patronale.getTauxApplique())
                .montantPatronal(patronale.getMontantFinal())
                .tauxPatronalAffiche(patronale.getTauxAffiche())

                .formuleCalcul(salariale.getFormuleCalcul())
                .isBareme(salariale.isBareme())
                .build();
    }

    private LignePaieDto getSalarialeFromPair(LignePaieDto ligne1, LignePaieDto ligne2) {
        // Vérifier par type d'abord
        if (ligne1.getType() == TypeElementPaie.RETENUE) return ligne1;
        if (ligne2.getType() == TypeElementPaie.RETENUE) return ligne2;

        // Vérifier par catégorie
        if (ligne1.getCategorie() == CategorieElement.COTISATION_SALARIALE) return ligne1;
        if (ligne2.getCategorie() == CategorieElement.COTISATION_SALARIALE) return ligne2;

        // Vérifier par désignation (celle qui ne contient pas les mots-clés patronaux)
        String des1 = ligne1.getDesignation().toUpperCase();
        String des2 = ligne2.getDesignation().toUpperCase();

        boolean ligne1IsPatronale = des1.contains("EMPLOYEUR") || des1.contains("PATRONAL") || des1.contains("PATRON");
        boolean ligne2IsPatronale = des2.contains("EMPLOYEUR") || des2.contains("PATRONAL") || des2.contains("PATRON");

        if (!ligne1IsPatronale && ligne2IsPatronale) return ligne1;
        if (ligne1IsPatronale && !ligne2IsPatronale) return ligne2;

        // Par défaut, retourner la première
        return ligne1;
    }

    private LignePaieDto getPatronaleFromPair(LignePaieDto ligne1, LignePaieDto ligne2) {
        // Retourner l'autre ligne
        LignePaieDto salariale = getSalarialeFromPair(ligne1, ligne2);
        return (salariale == ligne1) ? ligne2 : ligne1;
    }

    private String getGenericDesignation(String des1, String des2) {
        // Prendre la désignation la plus courte (généralement la plus générique)
        String shorter = des1.length() <= des2.length() ? des1 : des2;
        String longer = des1.length() > des2.length() ? des1 : des2;

        // 🔧 AMÉLIORATION : Essayer de prendre la plus générique
        String shorterUpper = shorter.toUpperCase();
        String longerUpper = longer.toUpperCase();

        if (!shorterUpper.contains("EMPLOYEUR") && !shorterUpper.contains("PATRONAL") && !shorterUpper.contains("PATRON")) {
            return shorter.trim();
        } else if (!longerUpper.contains("EMPLOYEUR") && !longerUpper.contains("PATRONAL") && !longerUpper.contains("PATRON")) {
            return longer.trim();
        }

        // Nettoyer les mentions spécifiques
        return shorter.replaceAll("(?i)\\s*(EMPLOYEUR|PATRONAL|PATRON|SALARIAL|SALARIE)\\s*", "").trim();
    }

    private Integer getValidAffichageOrdre(LignePaieDto ligne1, LignePaieDto ligne2) {
        return ligne1.getAffichageOrdre() != null ? ligne1.getAffichageOrdre() : ligne2.getAffichageOrdre();
    }
}