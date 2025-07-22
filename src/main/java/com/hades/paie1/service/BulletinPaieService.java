package com.hades.paie1.service;

import com.hades.paie1.dto.*;
import com.hades.paie1.enum1.*;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.*;
import com.hades.paie1.repository.*;
import com.hades.paie1.service.calculators.CotisationCalculator;
import com.hades.paie1.service.calculators.ImpotCalculator;
import com.hades.paie1.service.calculators.SalaireCalculator;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class BulletinPaieService {

    public BulletinPaieRepo bulletinRepo;
    private EmployeRepository employeRepo;

    private CotisationCalculator cotisationCalculator;
    private ImpotCalculator impotCalculator;
    private SalaireCalculator calculator;
    private MathUtils mathUtils;
    private  EmployeService employeService;
    private UserRepository userRepository;
    private EntrepriseRepository entrepriseRepository;
    private EmployePaieConfigRepository employePaieConfigRepo;
    private BulletinTemplateRepository bulletinTemplateRepository;
    private TemplateElementPaieConfigRepository templateRepository;

    private ElementPaieRepository elementPaieRepository;
    private PayrollDisplayService payrollDisplayService;
    private static final Logger logger = LoggerFactory.getLogger(BulletinPaieService.class);

    public BulletinPaieService (
            CotisationCalculator cotisationCalculator,
            ImpotCalculator impotCalculator,
            SalaireCalculator calculator,
            MathUtils mathUtils,
            BulletinPaieRepo bulletinRepo,
            EmployeRepository employeRepo,
            EmployeService employeService,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository,
            EmployePaieConfigRepository employePaieConfigRepo,
            TemplateElementPaieConfigRepository templateepository,
            BulletinTemplateRepository bulletinTemplate,
            ElementPaieRepository elementPaieRepository,
            PayrollDisplayService payrollDisplayService


    ){
       this.calculator = calculator;
       this.mathUtils = mathUtils;
       this.cotisationCalculator = cotisationCalculator;
       this.impotCalculator = impotCalculator;
       this.bulletinRepo= bulletinRepo;
       this.employeRepo= employeRepo;
       this.employeService= employeService;
       this.userRepository = userRepository;
       this.entrepriseRepository = entrepriseRepository;
       this.employePaieConfigRepo = employePaieConfigRepo;
       this.templateRepository = templateepository;
       this.bulletinTemplateRepository = bulletinTemplate;
       this.elementPaieRepository = elementPaieRepository;
       this.payrollDisplayService = payrollDisplayService;
    }



    //pas encore utilise mais pemet a ce que employe pour qui le bulletin est cree appartie bien a entreprise
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceNotFoundException("User not found: " + username)); // Utilisez RessourceNotFoundException ou UsernameNotFoundException
    }







    //methode pour calculer employer avec son bulletin
    @Transactional
    public BulletinPaie calculBulletin(BulletinPaie fiche) {
        System.out.println("Début calculBulletin :");
        System.out.println("Heures normales : " + fiche.getHeuresNormal());
        System.out.println("Taux horaire : " + fiche.getTauxHoraireInitial());
        System.out.println("Salaire base initial : " + fiche.getSalaireBaseInitial());

        Employe employe = employeRepo.findById(fiche.getEmploye().getId())
                .orElseThrow(() -> new RessourceNotFoundException("Employe non trouve"));

        if (fiche.getSalaireBaseInitial() == null) {
            fiche.setSalaireBaseInitial(employe.getSalaireBase());
        }

        if (fiche.getHeuresNormal() == null && employe.getHeuresContractuellesHebdomadaires() != null) {
            // Ex : heuresContractuellesHebdomadaires * 52 / 12 (moyenne mensuelle)
            BigDecimal heuresMensuelles = employe.getHeuresContractuellesHebdomadaires()
                    .multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            fiche.setHeuresNormal(heuresMensuelles);
        }

        if (fiche.getTauxHoraireInitial() == null) {
            if (fiche.getSalaireBaseInitial() != null && fiche.getHeuresNormal() != null && fiche.getHeuresNormal().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tauxHoraire = fiche.getSalaireBaseInitial().divide(fiche.getHeuresNormal(), 2, RoundingMode.HALF_UP);
                fiche.setTauxHoraireInitial(tauxHoraire);
            }
        }

        Entreprise entreprise = entrepriseRepository.findById(fiche.getEntreprise().getId())
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouve"));

        fiche.setEmploye(employe);
        fiche.setEntreprise(entreprise);
        fiche.clearLignesPaie(); // Nettoyer AVANT de recalculer

        BulletinTemplate template = bulletinTemplateRepository.findByEntrepriseAndIsDefaultTrue(entreprise)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun template par défaut"));

        LocalDate periodePourConfig = fiche.getDateDebutPeriode() != null ? fiche.getDateDebutPeriode() : LocalDate.now();

        // FIX 1: Traiter les éléments dans l'ordre correct et éviter les doublons
        Set<String> elementsTraites = new HashSet<>(); // Éviter les doublons

        // NOUVEAU: Ajouter automatiquement le salaire de base s'il n'existe pas déjà
        System.out.println("\n=== VERIFICATION ET AJOUT DU SALAIRE DE BASE ===");
        BigDecimal salaireBase = calculator.calculerSalaireBase(fiche);
        System.out.println("Salaire de base utilisé: " + salaireBase);

        // Marquer le salaire de base comme traité pour éviter qu'il soit retraité dans la boucle
        elementsTraites.add("SALAIRE_BASE_GAIN");

        // 1️⃣ BOUCLE 1 : GAINS SEULEMENT
        System.out.println("\n=== DEBUT CALCUL DES GAINS ===");
        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive() || config.getElementPaie().getType() != TypeElementPaie.GAIN) continue;

            ElementPaie element = config.getElementPaie();
            String elementKey = element.getCode() + "_" + element.getType(); // Clé unique

            // FIX 2: Éviter les doublons ET éviter de retraiter le salaire de base
            if (elementsTraites.contains(elementKey)) {
                System.out.println("Élément déjà traité : " + element.getCode());
                continue;
            }

            // NOUVEAU: Vérifier si c'est le salaire de base pour éviter la duplication
            if ("Salaire de Base".equals(element.getDesignation()) ||
                    "SALAIRE_BASE".equals(element.getCode()) ||
                    element.getCategorie() == CategorieElement.SALAIRE_DE_BASE) {
                System.out.println("Salaire de base déjà traité, passage au suivant");
                elementsTraites.add(elementKey);
                continue;
            }

            elementsTraites.add(elementKey);

            System.out.println("\nTraitement élément GAIN: " + element.getCode());

            List<EmployePaieConfig> configs = employePaieConfigRepo
                    .findActiveConfigForEmployeAndElementAndPeriode(employe, element.getId(), periodePourConfig);

            Optional<EmployePaieConfig> employeConfig = configs.stream().findFirst();

            System.out.println("Recherche config pour Employé " + employe.getId() + ", Element " + element.getId() + ", Période " + periodePourConfig);
            System.out.println("Nombre de configs trouvées: " + configs.size());
            for (EmployePaieConfig c : configs) {
                System.out.println("- id:" + c.getId() + ", valeur:" + c.getValeur() + ", dateDebut:" + c.getDateDebut() + ", dateFin:" + c.getDateFin());
            }

            BigDecimal valeur = BigDecimal.ZERO;
            BigDecimal montant = BigDecimal.ZERO;
            FormuleCalculType formule = config.getFormuleCalculOverride() != null ?
                    config.getFormuleCalculOverride() : element.getFormuleCalcul();

            if (formule == FormuleCalculType.MONTANT_FIXE) {
                valeur = employeConfig.map(EmployePaieConfig::getMontant)
                        .orElse(config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getTauxDefaut());
            } else if (formule == FormuleCalculType.POURCENTAGE_BASE) {
                valeur = employeConfig.map(EmployePaieConfig::getTaux)
                        .orElse(config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut());
            } else {
                valeur = BigDecimal.ZERO;
            }
            if (valeur == null) valeur = BigDecimal.ZERO;

            switch (formule) {
                case MONTANT_FIXE:
                    montant = valeur;
                    break;
                case NOMBRE_BASE_TAUX:
                    BigDecimal heures = fiche.getHeuresNormal() != null ? fiche.getHeuresNormal() : BigDecimal.ZERO;
                    BigDecimal taux = fiche.getTauxHoraireInitial() != null ? fiche.getTauxHoraireInitial() : BigDecimal.ZERO;
                    montant = heures.multiply(taux);
                    System.out.println("Calcul NOMBRE_BASE_TAUX - Heures: " + heures + ", Taux: " + taux + ", Montant: " + montant);
                    break;
                case POURCENTAGE_BASE:
                    // GAIN très rare ici, mais pour homogénéité :
                    BigDecimal base = element.isImpacteBaseCnps() && fiche.getBaseCnps() != null
                            ? fiche.getBaseCnps()
                            : fiche.getSalaireBrut() != null ? fiche.getSalaireBrut() : BigDecimal.ZERO;
                    montant = base.multiply(valeur);
                    break;
            }

            // FIX 3: Vérifier que le montant est > 0 avant d'ajouter
            if (montant.compareTo(BigDecimal.ZERO) > 0) {
                LigneBulletinPaie ligne = new LigneBulletinPaie();
                ligne.setElementPaie(element);
                ligne.setNombre(formule == FormuleCalculType.NOMBRE_BASE_TAUX ?
                        fiche.getHeuresNormal() != null ? fiche.getHeuresNormal() : BigDecimal.ONE :
                        BigDecimal.ONE);
                ligne.setTauxApplique(valeur);
                ligne.setMontantCalcul(montant);
                ligne.setMontantFinal(montant);
                fiche.addLignePaie(ligne);

                System.out.println("Ajout ligne GAIN: " + element.getCode() + ", Montant: " + montant);
            } else {
                System.out.println("❌ Montant zéro pour: " + element.getCode());
            }
        }

        // Calcul des avantages en nature
        BigDecimal totalAvantageNature = calculator.calculerTotalAvantageNature(fiche);
        if (totalAvantageNature.compareTo(BigDecimal.ZERO) > 0) {
            calculator.addLignePaieForElement(
                    fiche,
                    "Avantage en nature",
                    TypeElementPaie.GAIN,
                    CategorieElement.AVANTAGE_EN_NATURE,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    totalAvantageNature,
                    totalAvantageNature,
                    fiche.getSalaireBrut()
            );
        }

        // Calcul des heures supplémentaires
        calculator.calculerHeuresSupplementaires(fiche);
        calculator.calculerHeuresNuit(fiche);
        calculator.calculerHeuresFerie(fiche);
        calculator.calculerPrimeAnciennete(fiche);

        // 🔧 FIX 4: Calcul des bases APRÈS tous les gains
        System.out.println("\n=== CALCUL DES BASES (UNE SEULE FOIS) ===");
        fiche.setSalaireBrut(fiche.getTotalGains());
        System.out.println("Salaire Brut calculé: " + fiche.getSalaireBrut());

        // Calculer la base CNPS une seule fois
        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        fiche.setBaseCnps(baseCnps);
        System.out.println("Base CNPS calculée: " + baseCnps);

        // Calculer le salaire imposable une seule fois
        BigDecimal salaireImposable = calculator.calculSalaireImposable(fiche);
        fiche.setSalaireImposable(salaireImposable);
        System.out.println("Salaire Imposable calculé: " + salaireImposable);

        // 2️⃣ BOUCLE 2 : RETENUES & IMPOTS (ORDRE IMPORTANT)
        System.out.println("\n=== DEBUT CALCUL DES RETENUES (ORDRE SPECIFIQUE) ===");

        // ✅ ÉTAPE 1: Calculer d'abord l'IRPP (base pour CAC)
        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            if (element.getType() == TypeElementPaie.GAIN) continue;

            String code = element.getCode().toUpperCase();

            // Traiter UNIQUEMENT l'IRPP dans cette première passe
            if (!"IRPP".equalsIgnoreCase(code) && !code.contains("200")) continue;

            String elementKey = element.getCode() + "_" + element.getType();
            if (elementsTraites.contains(elementKey)) continue;
            elementsTraites.add(elementKey);

            System.out.println("\n🎯 Traitement prioritaire IRPP: " + element.getCode());

            LigneBulletinPaie ligne = new LigneBulletinPaie();
            calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, Optional.empty(), config);

            fiche.addLignePaie(ligne);
            System.out.println("✅ IRPP ajouté: " + ligne.getMontantFinal());
        }

        // ✅ ÉTAPE 2: Calculer ensuite CAC (qui dépend de l'IRPP)
        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            if (element.getType() == TypeElementPaie.GAIN) continue;

            String code = element.getCode().toUpperCase();

            // Traiter UNIQUEMENT CAC dans cette deuxième passe
            if (!"CAC".equalsIgnoreCase(code)) continue;

            String elementKey = element.getCode() + "_" + element.getType();
            if (elementsTraites.contains(elementKey)) continue;
            elementsTraites.add(elementKey);

            System.out.println("\n🎯 Traitement CAC (après IRPP): " + element.getCode());

            LigneBulletinPaie ligne = new LigneBulletinPaie();
            calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, Optional.empty(), config);

            fiche.addLignePaie(ligne);
            System.out.println("✅ CAC ajouté: " + ligne.getMontantFinal());
        }

        // ✅ ÉTAPE 3: Calculer toutes les autres retenues
        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            if (element.getType() == TypeElementPaie.GAIN) continue;

            String elementKey = element.getCode() + "_" + element.getType();
            if (elementsTraites.contains(elementKey)) {
                System.out.println("Élément déjà traité : " + element.getCode());
                continue;
            }
            elementsTraites.add(elementKey);

            System.out.println("\nTraitement élément RETENUE: " + element.getCode());

            List<EmployePaieConfig> configs = employePaieConfigRepo
                    .findActiveConfigForEmployeAndElementAndPeriode(employe, element.getId(), periodePourConfig);

            Optional<EmployePaieConfig> employeConfig = configs.stream().findFirst();

            LigneBulletinPaie ligne = new LigneBulletinPaie();
            calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, employeConfig, config);

            fiche.addLignePaie(ligne);
            System.out.println("✅ Ajout ligne RETENUE: " + element.getCode() + ", Montant: " + ligne.getMontantFinal());
        }

        // 🔧 FIX : Calcul des totaux finaux CORRECTS
        System.out.println("\n=== CALCUL DES TOTAUX FINAUX CORRIGES ===");

       // Total des GAINS (correct)
        fiche.setTotalGains(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.GAIN)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

       // Salaire Brut = Total des gains (correct)
        fiche.setSalaireBrut(fiche.getTotalGains());

       // Total COTISATIONS SALARIALES seulement (correct)
        fiche.setTotalCotisationsSalariales(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.COTISATION_SALARIALE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

       // Total IMPÔTS seulement (correct)
        fiche.setTotalImpots(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.IMPOT_SUR_REVENU ||
                        l.getElementPaie().getCategorie() == CategorieElement.IMPOT)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        // ✅ FIX PRINCIPAL : Total RETENUES SALARIALES = Cotisations + Impôts + Autres retenues
        BigDecimal totalRetenues = fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.RETENUE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        fiche.setTotalRetenuesSalariales(totalRetenues);

        // Charges patronales (correct)
        fiche.setTotalChargesPatronales(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.COTISATION_PATRONALE ||
                        l.getElementPaie().getType() == TypeElementPaie.CHARGE_PATRONALE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // ✅ NOUVEAU : Calcul de la cotisation CNPS totale (salariale + patronale)
        System.out.println("\n=== CALCUL COTISATION CNPS TOTALE ===");
        BigDecimal cotisationCnpsCalculee = cotisationCalculator.cotisationCnps(fiche);
        fiche.setCotisationCnps(cotisationCnpsCalculee);
        System.out.println("Cotisation CNPS totale calculée: " + cotisationCnpsCalculee);

       // ✅ CALCULS FINAUX CORRECTS :
       // Salaire Net avant impôt = Salaire Brut - Cotisations Salariales
        fiche.setSalaireNetAvantImpot(fiche.getSalaireBrut().subtract(fiche.getTotalCotisationsSalariales()));

       // Salaire Net à payer = Salaire Net avant impôt - Impôts
        fiche.setSalaireNetAPayer(fiche.getSalaireNetAvantImpot().subtract(fiche.getTotalImpots()));


      // Coût total employeur = Salaire Brut + Charges patronales
        fiche.setCoutTotalEmployeur(fiche.getSalaireBrut().add(fiche.getTotalChargesPatronales()));




      // 5. DEBUG - Ajoutez ces logs pour vérifier :
        System.out.println("=== VERIFICATION FINALE ===");
        System.out.println("Nombre de lignes GAIN: " + fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.GAIN).count());
        System.out.println("Nombre de lignes RETENUE: " + fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.RETENUE).count());
        System.out.println("Nombre de lignes CHARGE_PATRONALE: " + fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.CHARGE_PATRONALE).count());

        fiche.getLignesPaie().forEach(ligne -> {
            System.out.println("Ligne: " + ligne.getElementPaie().getCode() +
                    " - Type: " + ligne.getElementPaie().getType() +
                    " - Catégorie: " + ligne.getElementPaie().getCategorie() +
                    " - Montant: " + ligne.getMontantFinal());
        });

        System.out.println("\nTOTAUX CALCULÉS:");
        System.out.println("Total Gains: " + fiche.getTotalGains());
        System.out.println("Total Cotisations Salariales: " + fiche.getTotalCotisationsSalariales());
        System.out.println("Total Impôts: " + fiche.getTotalImpots());
        System.out.println("Total Retenues Salariales: " + fiche.getTotalRetenuesSalariales());
        System.out.println("Total Charges Patronales: " + fiche.getTotalChargesPatronales());
        System.out.println("Salaire Brut: " + fiche.getSalaireBrut());
        System.out.println("Salaire Net avant impôt: " + fiche.getSalaireNetAvantImpot());
        System.out.println("Salaire Net à payer: " + fiche.getSalaireNetAPayer());
        System.out.println("Coût total employeur: " + fiche.getCoutTotalEmployeur());


        fiche.setStatusBulletin(StatusBulletin.GÉNÉRÉ);
        fiche.setDateCreationBulletin(LocalDate.now());
        fiche.setAnnee(LocalDate.now().getYear());
        fiche.setMois(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH));

        return fiche;
    }

    // 🔧 MÉTHODE AJOUTÉE: Fusion des lignes patronales/salariales




    //Calcule le montant d'une retenue en utilisant CotisationCalculator quand c'est possible
    private void calculerMontantRetenue(LigneBulletinPaie ligne, ElementPaie element, FormuleCalculType formule, BigDecimal valeur, BulletinPaie fiche, Optional<EmployePaieConfig> employeConfig, TemplateElementPaieConfig config) {
        String code = element.getCode().toUpperCase();
        String designation = element.getDesignation() != null ? element.getDesignation().toUpperCase() : "";

        if (element.getCategorie() == CategorieElement.SALAIRE_DE_BASE ||
                "SALAIRE DE BASE".equals(designation) ||
                "SALAIRE_DE_BASE".equals(code)) {

            System.out.println("⚠️ SKIP: Salaire de base déjà géré par SalaireCalculator - " + designation);
            return; // Ne pas traiter ici !
        }

        BigDecimal montant = BigDecimal.ZERO;
        BigDecimal baseUtilisee = null;
        BigDecimal tauxApplique = valeur;
        String tauxAffiche = null;


        // Utiliser CotisationCalculator pour les cotisations spécifiques
        BigDecimal montantSpecifique = calculerCotisationSpecifique(code, designation, fiche);
        if (montantSpecifique != null) {
            System.out.println("Montant calculé via CotisationCalculator pour " + code + ": " + montantSpecifique);
            montant = montantSpecifique;
            baseUtilisee = determinerBaseCalcul(element, fiche);
            tauxApplique = obtenirTauxDepuisConstants(code, designation, element);

            if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
                baseUtilisee = impotCalculator.calculIrpp(fiche); // Base = IRPP
                tauxApplique = PaieConstants.TAUX_CAC; // Taux = 10%
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
                    tauxAffiche= null;
                    break;

                case POURCENTAGE_BASE:
                    baseUtilisee = determinerBaseCalcul(element, fiche);
                    tauxApplique = employeConfig.isPresent() && employeConfig.get().getTaux() != null ?
                            employeConfig.get().getTaux() :
                            (config.getTauxDefaut() != null ? config.getTauxDefaut() :
                                    (element.getTauxDefaut() != null ? element.getTauxDefaut() : BigDecimal.ZERO));

                    // Vérifier d'abord les constantes système
                    BigDecimal tauxConstant = obtenirTauxDepuisConstants(code, designation, element);
                    if (tauxConstant != null && tauxConstant.compareTo(BigDecimal.ZERO) > 0) {
                        tauxApplique = tauxConstant;
                    }

                    montant = baseUtilisee.multiply(tauxApplique);
                    if (tauxApplique != null) {
                        tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));
                    }
                    break;

                case NOMBRE_BASE_TAUX:
                    BigDecimal nombre = BigDecimal.ONE; // Par défaut
                    tauxApplique = employeConfig.isPresent() && employeConfig.get().getTaux() != null ?
                            employeConfig.get().getTaux() :
                            (config.getTauxDefaut() != null ? config.getTauxDefaut() :
                                    (element.getTauxDefaut() != null ? element.getTauxDefaut() : BigDecimal.ZERO));

                    montant = nombre.multiply(tauxApplique);
                    tauxAffiche = String.format("%.2f %%", tauxApplique.multiply(BigDecimal.valueOf(100)));

                    break;


                case TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal tauxDefaut = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefaut = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getTauxDefaut();
                    montant = tauxDefaut.multiply(montantDefaut);
                    tauxApplique = tauxDefaut;
                    tauxAffiche = String.format("%.2f %%", tauxDefaut.multiply(BigDecimal.valueOf(100)));
                    baseUtilisee = null;
                    break;

                case NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal nombreX = config.getNombreDefaut() != null ? config.getNombreDefaut() : BigDecimal.ONE;
                    BigDecimal tauxDefautX = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefautX = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getTauxDefaut();
                    montant = nombreX.multiply(tauxDefautX).multiply(montantDefautX);
                    tauxApplique = tauxDefautX;
                    tauxAffiche = String.format("%.2f %%", tauxDefautX.multiply(BigDecimal.valueOf(100)));
                    baseUtilisee = null;
                    ligne.setNombre(nombreX); // Remplis le nombre dans la ligne !
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

        // CORRECTION IMPORTANTE : Définir correctement le type d'élément
        TypeElementPaie typeElement = determinerTypeElement(element, code, designation);

        ligne.setElementPaie(element);
        ligne.setNombre(BigDecimal.ONE);
        ligne.setTauxApplique(tauxApplique);
        ligne.setMontantCalcul(montant);
        ligne.setMontantFinal(montant);
        ligne.setBaseApplique(baseUtilisee);
        ligne.setType(typeElement);
        ligne.setTauxAffiche(tauxAffiche);
        // Marquer les propriétés booléennes selon le type
        ligne.setEstGain(typeElement == TypeElementPaie.GAIN);
        ligne.setEstRetenue(typeElement == TypeElementPaie.RETENUE);
        ligne.setEstChargePatronale(typeElement == TypeElementPaie.CHARGE_PATRONALE);

        if (formule == FormuleCalculType.BAREME) {
            ligne.setBareme(true);
        }

        ligne.setFormuleCalcul(formule);


        System.out.println("Ligne créée - Code: " + code + ", Montant: " + montant +
                ", Taux: " + tauxApplique + ", Base: " + baseUtilisee + ", Type: " + typeElement +
                ", Formule: " + formule + ",TauxAffiche " +tauxAffiche);
    }


    private TypeElementPaie determinerTypeElement(ElementPaie element, String code, String designation) {
        // Si l'élément a déjà un type défini, l'utiliser
        if (element.getType() != null) {
            return element.getType();
        }

        if (code.contains("SALAIRE_BASE") || code.contains("SALAIRE_BRUT") ||
                designation.contains("SALAIRE DE BASE") || designation.contains("SALAIRE BRUT") ||
                element.getCategorie() == CategorieElement.SALAIRE_DE_BASE) {
            return TypeElementPaie.GAIN;
        }

        // Déterminer selon le code/désignation
        if (code.contains("EMPLOYEUR") || code.contains("PATRONAL") ||
                designation.contains("EMPLOYEUR") || designation.contains("PATRONAL")) {
            return TypeElementPaie.CHARGE_PATRONALE;
        }

        // Si c'est une cotisation patronale selon la catégorie
        if (element.getCategorie() == CategorieElement.COTISATION_PATRONALE) {
            return TypeElementPaie.CHARGE_PATRONALE;
        }

        // Par défaut, c'est une retenue salariale
        return TypeElementPaie.RETENUE;
    }
    // Méthode pour récupérer les taux depuis PaieConstants
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

        // CNPS Allocations Familiales
        if (code.contains("CNPS_ALLOCATIONS_FAMILIALES") ||
                code.contains("ALLOCATION_FAMILIALE_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ALLOCATION"))) {
            return PaieConstants.TAUX_CNPS_ALLOCATIONS_FAMILIALES;
        }

        // CNPS Accidents du Travail
        if (code.contains("CNPS_ACCIDENTS_TRAVAIL") ||
                code.contains("ACCIDENT_TRAVAIL_CNPS") ||
                (designation.contains("CNPS") && designation.contains("ACCIDENT"))) {
            return PaieConstants.TAUX_CNPS_ACCIDENTS_TRAVAIL;
        }

        // Crédit Foncier
        if (code.contains("CREDIT_FONCIER")) {
            if (code.contains("SALARIE") || code.contains("SALARIAL") || designation.contains("SALARI")) {
                return PaieConstants.TAUX_CREDIT_FONCIER_SALARIE;
            } else if (code.contains("PATRONAL") || designation.contains("PATRONAL")) {
                return PaieConstants.TAUX_CREDIT_FONCIER_PATRONAL;
            }
        }

        // FNE - Fonds National de l'Emploi
        if (code.contains("FNE") || (designation.contains("FONDS NATIONAL") && designation.contains("EMPLOI"))) {
            if (code.contains("SALARIE") || code.contains("SALARIAL") || designation.contains("SALARI")) {
                return PaieConstants.TAUX_FNE_SALARIE;
            } else if (code.contains("PATRONAL") || designation.contains("PATRONAL")) {
                return PaieConstants.TAUX_FNE_PATRONAL;
            }
        }

        // CAC - Centimes Additionnels Communaux
        if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
            return PaieConstants.TAUX_CAC;
        }

        // Pour les barèmes, retourner null (sera géré différemment)
        if ("IRPP".equalsIgnoreCase(code) || designation.contains("IRPP") ||
                code.contains("TAXE_COMMUNALE") || designation.contains("TAXE COMMUNALE") ||
                code.contains("REDEVANCE_AUDIOVISUELLE") || designation.contains("REDEVANCE AUDIOVISUELLE")) {
            return null;
        }

        return null; // Aucun taux trouvé dans les constantes
    }
    //  Modification de calculerCotisationSpecifique pour gérer les cotisations combinées
    private BigDecimal calculerCotisationSpecifique(String code, String designation, BulletinPaie fiche) {

        // CAC - Centimes Additionnels Communaux
        if ("CAC".equalsIgnoreCase(code) || designation.contains("CAC")) {
            return impotCalculator.calculCac(fiche);
        }

        if ("200".equals(code) || "IRPP".equalsIgnoreCase(code) || designation.contains("IRPP")) {
            return impotCalculator.calculIrpp(fiche); // ← Remplacez par la bonne méthode
        }

        // Taxe communale
        if (code.contains("TAXE_COMMUNALE") || designation.contains("TAXE COMMUNALE")) {
            return impotCalculator.calculTaxeCommunal(fiche);
        }

        // Redevance audiovisuelle
        if (code.contains("REDEVANCE_AUDIOVISUELLE") || designation.contains("REDEVANCE AUDIOVISUELLE")) {
            return impotCalculator.calculRedevanceAudioVisuelle(fiche);
        }

        // 🔧 CNPS Vieillesse - Séparation correcte employeur/salarié
        if (code.contains("CNPS_VIEILLESSE") || (designation.contains("CNPS") && designation.contains("VIEILLESSE"))) {
            // Si c'est spécifiquement pour l'employeur
            if (code.contains("EMPLOYEUR") || designation.contains("EMPLOYEUR") || code.contains("PATRONAL")) {
                return cotisationCalculator.calculCnpsVieillesseEmployeur(fiche);
            }
            // Si c'est spécifiquement pour le salarié
            else if (code.contains("SALARIE") || designation.contains("SALARIE") || code.contains("SALARIAL")) {
                return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
            }
            // 🔧 Si c'est générique, déterminer selon la catégorie de l'élément
            else {
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

                // Par défaut, retourner la cotisation salariale
                return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
            }
        }


        // Autres cotisations CNPS (restent inchangées)
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

        // Crédit Foncier
        if (code.contains("CREDIT_FONCIER_SALARIE") ||
                code.contains("CREDIT_FONCIER_SALARIAL") ||
                (designation.contains("CRÉDIT FONCIER") && designation.contains("SALARI"))) {
            return cotisationCalculator.calculCreditFoncierSalarie(fiche);
        }

        if (code.contains("CREDIT_FONCIER_PATRONAL") ||
                (designation.contains("CRÉDIT FONCIER") && designation.contains("PATRONAL"))) {
            return cotisationCalculator.calculCreditFoncierPatronal(fiche);
        }

        // Fonds National de l'Emploi
        if (code.contains("FNE_SALARIE") ||
                code.contains("FONDS_NATIONAL_EMPLOI") ||
                (designation.contains("FONDS NATIONAL") && designation.contains("EMPLOI"))) {
            return cotisationCalculator.calculFneSalaire(fiche);
        }

        if (code.contains("FNE_PATRONAL") ||
                (designation.contains("FONDS NATIONAL") && designation.contains("PATRONAL"))) {
            return cotisationCalculator.calculFnePatronal(fiche);
        }

        // Totaux globaux
        if (code.equals("TOTAL_CNPS") || designation.contains("TOTAL") && designation.contains("CNPS")) {
            return cotisationCalculator.cotisationCnps(fiche);
        }
        if (code.equals("TOTAL_CHARGES_PATRONALES") || designation.contains("TOTAL") && designation.contains("CHARGES PATRONALES")) {
            return cotisationCalculator.calculTotalChargesPatronales(fiche);
        }
        if (code.equals("TOTAL_RETENUES_SALARIE") || designation.contains("TOTAL") && designation.contains("RETENUES")) {
            return cotisationCalculator.calculTotalRetenuesSalaire(fiche);
        }

        return null; // Aucune cotisation spécifique trouvée
    }

     //Détermine la base de calcul selon l'élément de paie
    private BigDecimal determinerBaseCalcul(ElementPaie element, BulletinPaie fiche) {
        String code = element.getCode().toUpperCase();
        String designation = element.getDesignation() != null ? element.getDesignation().toUpperCase() : "";

        // CAC se base sur l'IRPP
        if ("CAC".equalsIgnoreCase(code)) {
            BigDecimal irppMontant = impotCalculator.calculIrpp(fiche);
            return irppMontant;
        }

        if (designation.contains("TAXE COMMUNALE") || code.contains("TAXE_COMMUNALE")) {
            return fiche.getSalaireBaseInitial() != null ? fiche.getSalaireBaseInitial() : BigDecimal.ZERO;
        }

        // CNPS se base sur la base CNPS
        if (designation.contains("CNPS") || code.contains("CNPS")) {
            return fiche.getBaseCnps() != null ? fiche.getBaseCnps() : BigDecimal.ZERO;
        }

        // Crédit foncier et FNE se basent sur le salaire imposable
        if (designation.contains("CRÉDIT FONCIER") || code.contains("CREDIT_FONCIER") ||
                designation.contains("FONDS NATIONAL") || code.contains("FNE") ||
                designation.contains("TAXE COMMUNALE") || code.contains("TAXE_COMMUNALE") ||
                designation.contains("REDEVANCE AUDIOVISUELLE") || code.contains("REDEVANCE_AUDIOVISUELLE")) {
            return fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
        }

        // IRPP se base sur le salaire imposable
        if ("IRPP".equalsIgnoreCase(code)) {
            return fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
        }

        // Par défaut, utiliser la configuration de l'élément
        return element.isImpacteBaseCnps() && fiche.getBaseCnps() != null
                ? fiche.getBaseCnps()
                : fiche.getSalaireImposable() != null ? fiche.getSalaireImposable() : BigDecimal.ZERO;
    }

    //Calcule le taux effectif basé sur le montant et la base

    private BigDecimal calculerTauxEffectif(BigDecimal montant, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montant.divide(base, 4, RoundingMode.HALF_UP);
    }

    //Calcule le montant pour les éléments en barème

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




    public BulletinPaieResponseDto convertToDto(BulletinPaie bulletinPaie) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();
        dto.setId(bulletinPaie.getId());
        dto.setTauxHoraire(bulletinPaie.getTauxHoraireInitial());
        dto.setHeuresNormal(bulletinPaie.getHeuresNormal());

        // Mapping des totaux principaux (qui sont déjà agrégés dans l'entité BulletinPaie)
        dto.setTotalGains(bulletinPaie.getTotalGains());
        dto.setSalaireBrut(bulletinPaie.getSalaireBrut());
        dto.setBaseCnps(bulletinPaie.getBaseCnps());
        dto.setSalaireImposable(bulletinPaie.getSalaireImposable());
        dto.setAvancesSurSalaires(bulletinPaie.getAvancesSurSalaires()); // Assurez-vous que ceci est géré comme une ligne de retenue si possible
        dto.setTotalImpots(bulletinPaie.getTotalImpots());
        dto.setTotalRetenuesSalariales(bulletinPaie.getTotalRetenuesSalariales());
        dto.setTotalChargesPatronales(bulletinPaie.getTotalChargesPatronales());
        dto.setSalaireNetAPayer(bulletinPaie.getSalaireNetAPayer());
        dto.setCoutTotalEmployeur(bulletinPaie.getCoutTotalEmployeur());
        dto.setCotisationCnps(bulletinPaie.getCotisationCnps());
        // Informations générales du bulletin
        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());
        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());

        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee() != null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A");
        }

        if(bulletinPaie.getMethodePaiement() != null) {
            dto.setMethodePaiement(bulletinPaie.getMethodePaiement().getDisplayValue());
        } else {
            dto.setMethodePaiement("Non specifiee");
        }

        // Mapping des objets complexes
        if(bulletinPaie.getEntreprise() != null){
            EntrepriseDto entrepriseDto = new EntrepriseDto();
            entrepriseDto.setId(bulletinPaie.getEntreprise().getId());
            entrepriseDto.setNom(bulletinPaie.getEntreprise().getNom());
            entrepriseDto.setNumeroSiret(bulletinPaie.getEntreprise().getNumeroSiret());
            entrepriseDto.setAdresseEntreprise(bulletinPaie.getEntreprise().getAdresseEntreprise());
            entrepriseDto.setTelephoneEntreprise(bulletinPaie.getEntreprise().getTelephoneEntreprise());
            entrepriseDto.setEmailEntreprise(bulletinPaie.getEntreprise().getEmailEntreprise());
            entrepriseDto.setLogoUrl(bulletinPaie.getEntreprise().getLogoUrl());
            dto.setEntreprise(entrepriseDto);
        }

        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = employeService.convertToDto(bulletinPaie.getEmploye());
            dto.setEmploye(employeDto);
        }

        // *** MAPPING DES LIGNES DE PAIE DYNAMIQUES ***
        // Récupérez les lignes brutes (celles de l'entité BulletinPaie)
        List<LigneBulletinPaie> lignesBrutes = bulletinPaie.getLignesPaie();

        // Appelez le PayrollDisplayService pour préparer les lignes (y compris la fusion)
        List<LignePaieDto> lignesPourAffichage = payrollDisplayService.prepareLignesPaieForDisplay(lignesBrutes);

        // Définissez les lignes préparées dans votre DTO de réponse
        dto.setLignesPaie(lignesPourAffichage);

        return dto;
    }

    // Nouvelle méthode pour convertir une LigneBulletinPaie en LignePaieDto
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
                tauxAffiche = "-";
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

    //Methode Crud
    @Transactional
    public BulletinPaieResponseDto saveBulletinPaie (BulletinPaie fiche){

        BulletinPaie calculatedAndFilledBulletin = calculBulletin(fiche);
        BulletinPaie savedBulletin = bulletinRepo.save(calculatedAndFilledBulletin);
        return  convertToDto(savedBulletin);
    }

    @Transactional
    public List<BulletinPaieResponseDto> getAllBulletinsPaie() {

        return bulletinRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    //pour gerer afficher en fonction  des roles
    @Transactional
    public BulletinPaieResponseDto getBulletinById(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouve avec l'id: " + id));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve avec le nom: " + currentUsername));
        if (currentUser.getRole() == Role.EMPLOYE) {
            Employe employe = employeRepo.findByUser(currentUser)
                    .orElseThrow(() -> new RessourceNotFoundException("No employee profile found for user: " + currentUsername));
            if (!bulletin.getEmploye().getId().equals(employe.getId())) {
                throw new AccessDeniedException("You are not authorized to view this bulletin.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            Entreprise entreprise = currentUser.getEntreprise();
            if (entreprise == null) {
                throw new IllegalStateException("Authenticated employer is not associated with an enterprise.");
            }
            if (!bulletin.getEntreprise().getId().equals(entreprise.getId())) {
                throw new AccessDeniedException("You are not authorized to view bulletins from another company.");
            }
        }

        return  convertToDto(bulletin);
    }



    @Transactional
    public Optional <BulletinPaieResponseDto> getBulletinPaieById (Long id){
        return bulletinRepo.findById(id)
                .map(this::convertToDto);
    }

    //cherche un employe
    public List<BulletinPaieResponseDto> getBulletinByEmployed(Long employeId){
        Employe employe = employeRepo.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID : " + employeId));
        return bulletinRepo.findByEmploye(employe).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    //mise a jour


    public void deleteBulletinPaie (Long id) {
        if (!bulletinRepo.existsById(id)){
            throw new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID :  "+ id);
        }
        bulletinRepo.deleteById(id);
    }

    @Transactional
    public boolean isBulletinOfCurrentUser(Long bulletinId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false; // Pas d'utilisateur authentifié
        }

        String username = authentication.getName();
        Optional<User> authenticatedUser = userRepository.findByUsername(username);

        if (authenticatedUser.isEmpty()) {
            return false; // Utilisateur non trouvé dans la base de données
        }

        User user = authenticatedUser.get();

        // On vérifie le rôle de l'utilisateur
        // Votre entité User a un seul champ `role` de type `Role` (enum).
        // Donc, nous devons comparer directement l'enum.
        boolean isEmployer = user.getRole() == Role.EMPLOYEUR; // Vérifie si le rôle est EMPLOYEUR
        boolean isEmployeRole = user.getRole() == Role.EMPLOYE; // Vérifie si le rôle est EMPLOYE

        if (isEmployer) {
            // L'utilisateur est un EMPLOYEUR. Il doit être lié à une entreprise.
            if (user.getEntreprise() == null) {
                System.out.println("DEBUG ENTREPRISE LINK: L'utilisateur EMPLOYEUR '" + username + "' n'est PAS lié à une entité Entreprise.");
                return false; // Un EMPLOYEUR doit avoir une entreprise associée
            }

            Long authenticatedEntrepriseId = user.getEntreprise().getId();
            System.out.println("DEBUG ENTREPRISE ID: ID de l'entreprise liée à l'utilisateur EMPLOYEUR: " + authenticatedEntrepriseId);

            // Tente de trouver le bulletin de paie
            return bulletinRepo.findById(bulletinId)
                    .map(bulletin -> {
                        // Vérifie si le bulletin est lié à un employé
                        if (bulletin.getEmploye() == null) {
                            System.out.println("DEBUG BULLETIN EMPLOYE: Bulletin ID " + bulletinId + " trouvé, mais SANS employé associé.");
                            return false;
                        }
                        Employe employeDuBulletin = bulletin.getEmploye();

                        // Vérifie si l'employé du bulletin est lié à une entreprise
                        if (employeDuBulletin.getEntreprise() == null) {
                            System.out.println("DEBUG BULLETIN ENTREPRISE: L'employé du bulletin ID " + bulletinId + " n'est PAS lié à une entité Entreprise.");
                            return false;
                        }
                        Long bulletinEntrepriseId = employeDuBulletin.getEntreprise().getId();
                        System.out.println("DEBUG BULLETIN ENTREPRISE: Bulletin ID " + bulletinId + " est lié à l'entreprise ID: " + bulletinEntrepriseId);

                        // Compare l'ID de l'entreprise de l'employeur avec l'ID de l'entreprise de l'employé du bulletin
                        boolean match = bulletinEntrepriseId.equals(authenticatedEntrepriseId);
                        System.out.println("DEBUG MATCH: L'ID de l'entreprise du bulletin correspond à l'ID de l'entreprise authentifiée? " + match);
                        return match;
                    })
                    .orElseGet(() -> {
                        System.out.println("DEBUG BULLETIN NOT FOUND: Bulletin avec l'ID " + bulletinId + " non trouvé.");
                        return false; // Bulletin non trouvé
                    });
        }
        else if (isEmployeRole) {
            // L'utilisateur est un EMPLOYE. Il ne peut voir que son propre bulletin.
            // L'utilisateur EMPLOYE doit être lié à une entité Employe.
            if (user.getEmploye() == null) {
                System.out.println("DEBUG EMPLOYE LINK: L'utilisateur '" + username + "' avec le rôle EMPLOYE n'est PAS lié à une entité Employe.");
                return false;
            }
            Long authenticatedEmployeId = user.getEmploye().getId();
            System.out.println("DEBUG EMPLOYE ID: ID de l'employé lié à l'utilisateur EMPLOYE: " + authenticatedEmployeId);

            return bulletinRepo.findById(bulletinId)
                    .map(bulletin -> {
                        boolean match = bulletin.getEmploye() != null && bulletin.getEmploye().getId().equals(authenticatedEmployeId);
                        System.out.println("DEBUG MATCH: L'ID de l'employé du bulletin correspond à l'ID de l'employé authentifié? " + match);
                        return match;
                    })
                    .orElseGet(() -> {
                        System.out.println("DEBUG BULLETIN NOT FOUND: Bulletin avec l'ID " + bulletinId + " non trouvé.");
                        return false;
                    });
        }

        // Si l'utilisateur n'a ni le rôle EMPLOYEUR ni EMPLOYE (et pas ADMIN qui est géré par @PreAuthorize),
        // il n'a pas accès via cette méthode.
        System.out.println("DEBUG: L'utilisateur n'a pas les rôles ou la configuration nécessaire pour cette vérification.");
        return false;
    }

    @Transactional
    public List<BulletinPaieEmployeurDto> getBulletinsForEmployer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.EMPLOYEUR) {
            throw new AccessDeniedException("Only employers can view their company's bulletins.");
        }

        Entreprise entreprise = currentUser.getEntreprise();
        if (entreprise == null) {
            throw new IllegalStateException("Authenticated employer is not associated with an enterprise.");
        }

        // Récupérez les bulletins de la base de données, triés par date de création
        List<BulletinPaie> bulletins = bulletinRepo.findByEntrepriseOrderByDateCreationBulletinDesc(entreprise);

        //  Tri avec Map inline
        Map<StatusBulletin, Integer> statusOrder = Map.of(
                StatusBulletin.VALIDÉ, 1,
                StatusBulletin.GÉNÉRÉ, 2,
                StatusBulletin.ENVOYÉ, 3,
                StatusBulletin.ARCHIVÉ, 4,
                StatusBulletin.ANNULÉ, 5
        );

        bulletins.sort(Comparator.comparing((BulletinPaie b) -> statusOrder.getOrDefault(b.getStatusBulletin(), 6))
                .thenComparing(BulletinPaie::getDateCreationBulletin, Comparator.reverseOrder()));

        return bulletins.stream()
                .map(this::convertToEmployeurDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public List<BulletinPaieResponseDto> getMyBulletins() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.EMPLOYE) {
            throw new AccessDeniedException("Only employees can view their own bulletins.");
        }

        Employe employe = employeRepo.findByUser(currentUser)
                .orElseThrow(() -> new RessourceNotFoundException("No employee profile found for user: " + currentUsername));

        //Defini les statut qui doivent etre visibles par employe
        List<StatusBulletin> visibleStatuses = List.of(StatusBulletin.ENVOYÉ, StatusBulletin.ARCHIVÉ);

        return bulletinRepo.findByEmployeAndStatusBulletinIn(employe, visibleStatuses).stream()
                .map(this::convertToDto) // Using the existing convertToDto for employee's own bulletins
                .collect(Collectors.toList());
    }

    //methode pour la recherche
    @Transactional
    public List<BulletinPaieEmployeurDto> searchBulletinsForEmployer(String searchTerm) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.EMPLOYEUR) {
            throw new AccessDeniedException("Only employers can view their company's bulletins.");
        }

        Entreprise entreprise = currentUser.getEntreprise();
        if (entreprise == null) {
            throw new IllegalStateException("Authenticated employer is not associated with an enterprise.");
        }

        // Récupérez les bulletins en utilisant un mot-clé de recherche
        List<BulletinPaie> bulletins = bulletinRepo.findByEntrepriseAndEmploye_NomContainingIgnoreCaseOrEmploye_PrenomContainingIgnoreCaseOrEmploye_MatriculeContainingIgnoreCaseOrderByDateCreationBulletinDesc(
                entreprise, searchTerm, searchTerm, searchTerm);

        // OPTION 1: Tri avec Map inline
        Map<StatusBulletin, Integer> statusOrder = Map.of(
                StatusBulletin.VALIDÉ, 1,
                StatusBulletin.GÉNÉRÉ, 2,
                StatusBulletin.ENVOYÉ, 3,
                StatusBulletin.ARCHIVÉ, 4,
                StatusBulletin.ANNULÉ, 5
        );

        bulletins.sort(Comparator.comparing((BulletinPaie b) -> statusOrder.getOrDefault(b.getStatusBulletin(), 6))
                .thenComparing(BulletinPaie::getDateCreationBulletin, Comparator.reverseOrder()));

        return bulletins.stream()
                .map(this::convertToEmployeurDto)
                .collect(Collectors.toList());
    }

    //Statut
    @Transactional
    public BulletinPaieResponseDto validerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouve avec l'ID :" +id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw  new AccessDeniedException("Seuls les employeur ou admin peuvent valider un bulletin ");
        }

        if (currentUser.getRole() == Role.EMPLOYEUR &&!bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à valider les bulletins d'une autre entreprise.");
        }


        // Vérifier la transition de statut
        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("GÉNÉRÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être validé que s'il est au statut 'Généré'. Statut actuel : " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.VALIDÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    //enoye bulletin
    @Transactional
    public BulletinPaieResponseDto envoyerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent envoyer un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à envoyer les bulletins d'une autre entreprise.");
        }

        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("VALIDÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être envoyé que s'il est au statut 'Validé'. Statut actuel : " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.ENVOYÉ);
//        bulletin.setDatePaiement(LocalDate.now()); // Définir la date de paiement lors de l'envoi
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    @Transactional
    public BulletinPaieResponseDto archiverBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent archiver un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à archiver les bulletins d'une autre entreprise.");
        }



        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("ENVOYÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être archivé que s'il est au statut 'Validé' ou 'Envoyé'. Statut actuel :: " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.ARCHIVÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    @Transactional
    public BulletinPaieResponseDto annulerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent annuler un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à annuler les bulletins d'une autre entreprise.");
        }

        if (bulletin.getStatusBulletin() != null && "ARCHIVÉ".equalsIgnoreCase(bulletin.getStatusBulletin().toString().trim())) {
            throw new IllegalStateException("Un bulletin archivé ne peut pas être annulé directement. Il doit être désarchivé ou une nouvelle rectification doit être créée.");
        }



        bulletin.setStatusBulletin(StatusBulletin.ANNULÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }





    @Transactional
    public long countBulletinsForAuthenticatedEmployer(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUSer = userRepository.findByUsername(currentUsername)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: " +currentUsername));
        if(currentUSer.getRole() != Role.EMPLOYEUR && currentUSer.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only emplyers or admis can view bulletin count.");
        }
        if (currentUSer.getRole() == Role.ADMIN) {
            return bulletinRepo.count();
        } else {
            Entreprise entreprise = currentUSer.getEntreprise();
            if(entreprise == null) {
                throw new IllegalStateException("Authenticated employer is not associated with an enterprise");
            }
            return bulletinRepo.countByEntreprise(entreprise);
        }
    }



    private BulletinPaieResponseDto convertToResponseDto(BulletinPaie bulletinPaie) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();

        // Mapping des champs de base
        dto.setId(bulletinPaie.getId());
        dto.setTauxHoraire(bulletinPaie.getTauxHoraireInitial());
        dto.setHeuresNormal(bulletinPaie.getHeuresNormal());
        dto.setSalaireBrut(bulletinPaie.getSalaireBrut());
        dto.setSalaireImposable(bulletinPaie.getSalaireImposable());
        dto.setBaseCnps(bulletinPaie.getBaseCnps());
        dto.setCoutTotalEmployeur(bulletinPaie.getCoutTotalEmployeur());
        dto.setCotisationCnps(bulletinPaie.getCotisationCnps());
        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());
        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());

        // Gestion de la période de paie
        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee() != null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A");
        }

        // Gestion de la méthode de paiement
        if (bulletinPaie.getMethodePaiement() != null) {
            dto.setMethodePaiement(bulletinPaie.getMethodePaiement().getDisplayValue());
        } else {
            dto.setMethodePaiement("Non spécifiée");
        }

        // Mapping de l'employé
        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = employeService.convertToDto(bulletinPaie.getEmploye());
            dto.setEmploye(employeDto);
        }

        // Mapping de l'entreprise
        if (bulletinPaie.getEmploye() != null && bulletinPaie.getEmploye().getEntreprise() != null) {
            EntrepriseDto entrepriseDto = convertEntrepriseToDto(bulletinPaie.getEmploye().getEntreprise());
            dto.setEntreprise(entrepriseDto);
        }

        // *** MAPPING DYNAMIQUE DES LIGNES DE PAIE ***
        if (bulletinPaie.getLignesPaie() != null) {
            List<LignePaieDto> lignesPaieDto = bulletinPaie.getLignesPaie().stream()
                    .map(this::convertLigneBulletinPaieToDto)
                    .collect(Collectors.toList());
            dto.setLignesPaie(lignesPaieDto);
        }

        return dto;
    }



    private EntrepriseDto convertEntrepriseToDto(Entreprise entreprise) {
        EntrepriseDto dto = new EntrepriseDto();
        dto.setId(entreprise.getId());
        dto.setNom(entreprise.getNom());
        dto.setNumeroSiret(entreprise.getNumeroSiret());
        dto.setAdresseEntreprise(entreprise.getAdresseEntreprise());
        dto.setTelephoneEntreprise(entreprise.getTelephoneEntreprise());
        dto.setEmailEntreprise(entreprise.getEmailEntreprise());
        dto.setLogoUrl(entreprise.getLogoUrl());
        return dto;
    }












    private BulletinPaieEmployeurDto convertToEmployeurDto(BulletinPaie bulletinPaie) {
        BulletinPaieEmployeurDto dto = new BulletinPaieEmployeurDto();
        dto.setId(bulletinPaie.getId());

        // Mappage des champs spécifiques qui sont des inputs ou des totaux agrégés
        dto.setSalaireBaseInitial(bulletinPaie.getSalaireBaseInitial());
        dto.setTauxHoraireInitial(bulletinPaie.getTauxHoraireInitial());
        dto.setHeuresNormal(bulletinPaie.getHeuresNormal());
        dto.setHeuresSup(bulletinPaie.getHeuresSup()); // Assurez-vous que bulletinPaie.getHeuresSup() existe
        dto.setHeuresNuit(bulletinPaie.getHeuresNuit());
        dto.setHeuresFerie(bulletinPaie.getHeuresFerie());
        dto.setAvancesSurSalaires(bulletinPaie.getAvancesSurSalaires());

        dto.setTotalGains(bulletinPaie.getTotalGains()); // Ancien totalPrimes
        dto.setSalaireBrut(bulletinPaie.getSalaireBrut());
        dto.setSalaireImposable(bulletinPaie.getSalaireImposable());
        dto.setBaseCnps(bulletinPaie.getBaseCnps());
        dto.setTotalRetenuesSalariales(bulletinPaie.getTotalRetenuesSalariales()); // Ancien totalRetenues
        dto.setTotalImpots(bulletinPaie.getTotalImpots()); // Nouveau champ
        dto.setTotalChargesPatronales(bulletinPaie.getTotalChargesPatronales());
        dto.setCotisationCnps(bulletinPaie.getCotisationCnps());
        dto.setCoutTotalEmployeur(bulletinPaie.getCoutTotalEmployeur());
        dto.setSalaireNetAPayer(bulletinPaie.getSalaireNetAPayer());
        // Informations générales du bulletin
        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());
        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());

        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee() != null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A");
        }

        if(bulletinPaie.getMethodePaiement() != null) {
            dto.setMethodePaiement(bulletinPaie.getMethodePaiement().getDisplayValue());
        }else {
            dto.setMethodePaiement("Non specifiee");
        }

        // Convert Employe to EmployeResponseDto
        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = employeService.convertToDto(bulletinPaie.getEmploye()); // Réutiliser la méthode existante
            dto.setEmploye(employeDto);
        }

        // *** MAPPING DES LIGNES DE PAIE DYNAMIQUES POUR EMPLOYEUR ***
        List<LignePaieDto> lignesPaieDto = bulletinPaie.getLignesPaie().stream()
                .map(this::convertLigneBulletinPaieToDto) // Réutiliser la méthode de conversion
                .collect(Collectors.toList());
        dto.setLignesPaie(lignesPaieDto);

        return dto;
    }




    @Transactional
    public List<BulletinPaieResponseDto> getBulletinsFotCurrentUser() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
              throw new IllegalArgumentException("Aucun utilisateur authentifie");
          }
          String username = authentication.getName();

          Optional<User> currentUserOptinal = userRepository.findByUsername(username);

          if (currentUserOptinal.isEmpty()) {
              throw new RessourceNotFoundException("Utilisateur non trouve avec le nom d'utilisateur: " + username);
          }
          User currentUser = currentUserOptinal.get();

          if (currentUser.getRole().name().equals("ADMIN")) {
              return bulletinRepo.findAll().stream()
                      .map(this::convertToDto)
                      .collect(Collectors.toList());
          } else if (currentUser.getRole().name().equals("EMPLOYE")) {
              if (currentUser.getEmploye() == null) {
                  throw new IllegalStateException("Le compte employe n'est pas lie a un enregistrement d'employe.");
              }
              Employe employe = currentUser.getEmploye();
              return bulletinRepo.findByEmploye(employe).stream()
                      .map(this::convertToDto)
                      .collect(Collectors.toList());
          } else {
              throw new IllegalStateException("Role d'utilisateur non pris en charge pour la recuperation des bulletins");
          }

    }









    public BulletinPaieResponseDto updateBulletinPaie (Long id, BulletinPaie updatedBulletinPaie){

        BulletinPaie existingBulletinPaie = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID :  "+ id));

        existingBulletinPaie.setSalaireBaseInitial(updatedBulletinPaie.getSalaireBaseInitial());
        existingBulletinPaie.setTauxHoraireInitial(updatedBulletinPaie.getTauxHoraireInitial());
        existingBulletinPaie.setHeuresNormal(updatedBulletinPaie.getHeuresNormal());
        existingBulletinPaie.setHeuresSup(updatedBulletinPaie.getHeuresSup()); // Utilise heuresSup
        existingBulletinPaie.setHeuresNuit(updatedBulletinPaie.getHeuresNuit());
        existingBulletinPaie.setHeuresFerie(updatedBulletinPaie.getHeuresFerie());
        existingBulletinPaie.setPrimeAnciennete(updatedBulletinPaie.getPrimeAnciennete()); // Garde primeAnciennete si toujours présent
        existingBulletinPaie.setAvancesSurSalaires(updatedBulletinPaie.getAvancesSurSalaires()); // Maintenu car c'est une valeur d'entrée/déduction spécifique

        if (updatedBulletinPaie.getEmploye() != null && updatedBulletinPaie.getEmploye().getId() != null
                && !existingBulletinPaie.getEmploye().getId().equals(updatedBulletinPaie.getEmploye().getId())) {
            Employe newEmploye = employeRepo.findById(updatedBulletinPaie.getEmploye().getId())
                    .orElseThrow(() -> new RessourceNotFoundException("Nouveau Employe non trouve avec id :" + updatedBulletinPaie.getEmploye().getId()));
            existingBulletinPaie.setEmploye(newEmploye);
        }
        BulletinPaie calculBulletinUpdate = calculBulletin(existingBulletinPaie);

        // Sauvegarde du bulletin de paie mis à jour et recalculé
        BulletinPaie savedBulletin = bulletinRepo.save(calculBulletinUpdate);

        return convertToDto(savedBulletin);
    }
}
