package com.hades.paie1.service;

import com.hades.paie1.dto.*;
import com.hades.paie1.enum1.*;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.*;
import com.hades.paie1.repository.*;
import com.hades.paie1.service.calculators.CotisationCalculator;
import com.hades.paie1.service.calculators.ImpotCalculator;
import com.hades.paie1.service.calculators.RetenueCalculator;
import com.hades.paie1.service.calculators.SalaireCalculator;
import com.hades.paie1.utils.MathUtils;
import com.hades.paie1.utils.PaieConstants;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public final  BulletinPaieRepo bulletinRepo;
    private final  EmployeRepository employeRepo;

    private final  CotisationCalculator cotisationCalculator;

    private final  SalaireCalculator calculator;

    private  final  EmployeService employeService;
    private  final UserRepository userRepository;
    private  final EntrepriseRepository entrepriseRepository;
    private final EmployePaieConfigRepository employePaieConfigRepo;
    private final BulletinTemplateRepository bulletinTemplateRepository;
    private final TemplateElementPaieConfigRepository templateRepository;

    private final ElementPaieRepository elementPaieRepository;
    private final PayrollDisplayService payrollDisplayService;

    private final RetenueCalculator retenueCalculator;
    private final AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(BulletinPaieService.class);

    public BulletinPaieService (
            CotisationCalculator cotisationCalculator,
            SalaireCalculator calculator,
            BulletinPaieRepo bulletinRepo,
            EmployeRepository employeRepo,
            EmployeService employeService,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository,
            EmployePaieConfigRepository employePaieConfigRepo,
            TemplateElementPaieConfigRepository templateepository,
            BulletinTemplateRepository bulletinTemplate,
            ElementPaieRepository elementPaieRepository,
            PayrollDisplayService payrollDisplayService,
            RetenueCalculator retenueCalculator,
            AuditLogService auditLogService


    ){
       this.calculator = calculator;
       this.cotisationCalculator = cotisationCalculator;
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
       this.retenueCalculator = retenueCalculator;
       this.auditLogService = auditLogService;
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
        fiche.setLogoEntrepriseSnapshot(entreprise.getLogoUrl());
        fiche.clearLignesPaie(); // Nettoyer AVANT de recalculer

        BulletinTemplate template = bulletinTemplateRepository.findByEntrepriseAndIsDefaultTrueWithElements(entreprise)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun template par défaut"));

        // Force l'initialisation de la collection LAZY
        Hibernate.initialize(template.getElementsConfig());

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
            Hibernate.initialize(element); // PATCH: initialize ElementPaie
            String elementKey = element.getCode() + "_" + element.getType();

            if (elementsTraites.contains(elementKey)) {
                System.out.println("Élément déjà traité : " + element.getCode());
                continue;
            }

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
                    break;
                case POURCENTAGE_BASE:
                    BigDecimal base = element.isImpacteBaseCnps() && fiche.getBaseCnps() != null
                            ? fiche.getBaseCnps()
                            : fiche.getSalaireBrut() != null ? fiche.getSalaireBrut() : BigDecimal.ZERO;
                    montant = base.multiply(valeur);
                    break;
                case TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal tauxDefaut = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefaut = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getMontantDefaut();
                    montant = tauxDefaut.multiply(montantDefaut);
                    valeur = tauxDefaut;
                    break;
                case NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT:
                    BigDecimal nombreDefaut = config.getNombreDefaut() != null ? config.getNombreDefaut() : BigDecimal.ONE;
                    BigDecimal tauxDefautX = config.getTauxDefaut() != null ? config.getTauxDefaut() : element.getTauxDefaut();
                    BigDecimal montantDefautX = config.getMontantDefaut() != null ? config.getMontantDefaut() : element.getMontantDefaut();
                    montant = nombreDefaut.multiply(tauxDefautX).multiply(montantDefautX);
                    valeur = tauxDefautX;
                    break;

            }

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

                Hibernate.initialize(ligne.getElementPaie()); // PATCH: initialize ligne.elementPaie

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

        calculator.calculerHeuresSupplementaires(fiche);
        calculator.calculerHeuresNuit(fiche);
        calculator.calculerHeuresFerie(fiche);
        calculator.calculerPrimeAnciennete(fiche);

        System.out.println("\n=== CALCUL DES BASES (UNE SEULE FOIS) ===");
        fiche.setSalaireBrut(fiche.getTotalGains());
        System.out.println("Salaire Brut calculé: " + fiche.getSalaireBrut());

        BigDecimal baseCnps = calculator.calculBaseCnps(fiche);
        fiche.setBaseCnps(baseCnps);
        System.out.println("Base CNPS calculée: " + baseCnps);

        BigDecimal salaireImposable = calculator.calculSalaireImposable(fiche);
        fiche.setSalaireImposable(salaireImposable);
        System.out.println("Salaire Imposable calculé: " + salaireImposable);

        System.out.println("\n=== DEBUT CALCUL DES RETENUES (ORDRE SPECIFIQUE) ===");

        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            Hibernate.initialize(element); // PATCH: initialize ElementPaie
            if (element.getType() == TypeElementPaie.GAIN) continue;

            String code = element.getCode().toUpperCase();

            if (!"IRPP".equalsIgnoreCase(code) && !code.contains("200")) continue;

            String elementKey = element.getCode() + "_" + element.getType();
            if (elementsTraites.contains(elementKey)) continue;
            elementsTraites.add(elementKey);

            System.out.println("\n🎯 Traitement prioritaire IRPP: " + element.getCode());

            LigneBulletinPaie ligne = new LigneBulletinPaie();
            retenueCalculator.calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, Optional.empty(), config);

            fiche.addLignePaie(ligne);
            Hibernate.initialize(ligne.getElementPaie()); // PATCH: initialize ligne.elementPaie
            System.out.println("✅ IRPP ajouté: " + ligne.getMontantFinal());
        }

        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            Hibernate.initialize(element); // PATCH: initialize ElementPaie
            if (element.getType() == TypeElementPaie.GAIN) continue;

            String code = element.getCode().toUpperCase();

            if (!"CAC".equalsIgnoreCase(code)) continue;

            String elementKey = element.getCode() + "_" + element.getType();
            if (elementsTraites.contains(elementKey)) continue;
            elementsTraites.add(elementKey);

            System.out.println("\n🎯 Traitement CAC (après IRPP): " + element.getCode());

            LigneBulletinPaie ligne = new LigneBulletinPaie();
            retenueCalculator.calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, Optional.empty(), config);

            fiche.addLignePaie(ligne);
            Hibernate.initialize(ligne.getElementPaie()); // PATCH: initialize ligne.elementPaie
            System.out.println("✅ CAC ajouté: " + ligne.getMontantFinal());
        }

        for (TemplateElementPaieConfig config : template.getElementsConfig()) {
            if (!config.isActive()) continue;

            ElementPaie element = config.getElementPaie();
            Hibernate.initialize(element); // PATCH: initialize ElementPaie
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
            retenueCalculator.calculerMontantRetenue(ligne, element, config.getFormuleCalculOverride() != null ?
                            config.getFormuleCalculOverride() : element.getFormuleCalcul(),
                    BigDecimal.ZERO, fiche, employeConfig, config);

            fiche.addLignePaie(ligne);
            Hibernate.initialize(ligne.getElementPaie()); // PATCH: initialize ligne.elementPaie
            System.out.println("✅ Ajout ligne RETENUE: " + element.getCode() + ", Montant: " + ligne.getMontantFinal());
        }

        System.out.println("\n=== CALCUL DES TOTAUX FINAUX CORRIGES ===");

        fiche.setTotalGains(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.GAIN)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        fiche.setSalaireBrut(fiche.getTotalGains());
        fiche.setTotalCotisationsSalariales(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.COTISATION_SALARIALE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        fiche.setTotalImpots(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.IMPOT_SUR_REVENU ||
                        l.getElementPaie().getCategorie() == CategorieElement.IMPOT)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalRetenues = fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getType() == TypeElementPaie.RETENUE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        fiche.setTotalRetenuesSalariales(totalRetenues);

        fiche.setTotalChargesPatronales(fiche.getLignesPaie().stream()
                .filter(l -> l.getElementPaie().getCategorie() == CategorieElement.COTISATION_PATRONALE ||
                        l.getElementPaie().getType() == TypeElementPaie.CHARGE_PATRONALE)
                .map(LigneBulletinPaie::getMontantFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        System.out.println("\n=== CALCUL COTISATION CNPS TOTALE ===");
        BigDecimal cotisationCnpsCalculee = cotisationCalculator.cotisationCnps(fiche);
        fiche.setCotisationCnps(cotisationCnpsCalculee);
        System.out.println("Cotisation CNPS totale calculée: " + cotisationCnpsCalculee);

        fiche.setSalaireNetAvantImpot(fiche.getSalaireBrut().subtract(fiche.getTotalCotisationsSalariales()));
        fiche.setSalaireNetAPayer(fiche.getSalaireNetAvantImpot().subtract(fiche.getTotalImpots()));
        fiche.setCoutTotalEmployeur(fiche.getSalaireBrut().add(fiche.getTotalChargesPatronales()));

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

    public BulletinPaieResponseDto convertToDto(BulletinPaie bulletinPaie) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();

        dto.setId(bulletinPaie.getId());
        dto.setSalaireBaseInitial(nvl(bulletinPaie.getSalaireBaseInitial()));
        dto.setTauxHoraire(nvl(bulletinPaie.getTauxHoraireInitial()));
        dto.setHeuresNormal(nvl(bulletinPaie.getHeuresNormal()));
        dto.setHeuresSup(nvl(bulletinPaie.getHeuresSup()));
        dto.setHeuresNuit(nvl(bulletinPaie.getHeuresNuit()));
        dto.setHeuresFerie(nvl(bulletinPaie.getHeuresFerie()));
        dto.setPrimeAnciennete(nvl(bulletinPaie.getPrimeAnciennete()));
        dto.setAvancesSurSalaires(nvl(bulletinPaie.getAvancesSurSalaires()));

        // Totaux principaux (toujours BigDecimal.ZERO si null)
        dto.setTotalGains(nvl(bulletinPaie.getTotalGains()));
        dto.setSalaireBrut(nvl(bulletinPaie.getSalaireBrut()));
        dto.setSalaireImposable(nvl(bulletinPaie.getSalaireImposable()));
        dto.setBaseCnps(nvl(bulletinPaie.getBaseCnps()));
        dto.setTotalRetenuesSalariales(nvl(bulletinPaie.getTotalRetenuesSalariales()));
        dto.setTotalImpots(nvl(bulletinPaie.getTotalImpots()));
        dto.setTotalChargesPatronales(nvl(bulletinPaie.getTotalChargesPatronales()));
        dto.setCotisationCnps(nvl(bulletinPaie.getCotisationCnps()));
        dto.setCoutTotalEmployeur(nvl(bulletinPaie.getCoutTotalEmployeur()));
        dto.setSalaireNetAPayer(nvl(bulletinPaie.getSalaireNetAPayer()));
        dto.setSalaireNetAvantImpot(nvl(bulletinPaie.getSalaireNetAvantImpot()));
        dto.setLogoEntrepriseSnapshot(bulletinPaie.getLogoEntrepriseSnapshot());
        // Infos générales
        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());
        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());

        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee() != null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A");
        }
        if (bulletinPaie.getMethodePaiement() != null) {
            dto.setMethodePaiement(bulletinPaie.getMethodePaiement().getDisplayValue());
        } else {
            dto.setMethodePaiement("Non spécifiée");
        }

        // Entreprise
        if (bulletinPaie.getEntreprise() != null) {
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

        // Employé
        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = employeService.convertToDto(bulletinPaie.getEmploye());
            dto.setEmploye(employeDto);
        }

        // Lignes de paie dynamiques
        List<LigneBulletinPaie> lignesBrutes = bulletinPaie.getLignesPaie();
        List<LignePaieDto> lignesPourAffichage = payrollDisplayService.prepareLignesPaieForDisplay(lignesBrutes);
        dto.setLignesPaie(lignesPourAffichage);

        return dto;
    }
    private static BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    // Nouvelle méthode pour convertir une LigneBulletinPaie en LignePaieDto
    private LignePaieDto convertLigneBulletinPaieToDto(LigneBulletinPaie ligne) {
        Integer affichageOrdre = null;
        String tauxAffiche = null;

        // Correction ici !
        if (ligne.getElementPaie() != null) {
            FormuleCalculType formule = ligne.getElementPaie().getFormuleCalcul();
            String designation = ligne.getElementPaie().getDesignation();

            if (formule == FormuleCalculType.BAREME) {
                tauxAffiche = "BARÈME";
            } else if (formule == FormuleCalculType.MONTANT_FIXE
                    || designation.equalsIgnoreCase("Salaire de base")
                    || ligne.getElementPaie().getCategorie() == CategorieElement.SALAIRE_DE_BASE) {
                // Pour le salaire de base et les montants fixes, ne pas afficher de taux
                tauxAffiche = "--";
            } else if (ligne.getTauxApplique() != null && ligne.getTauxApplique().compareTo(BigDecimal.ZERO) != 0) {
                tauxAffiche = ligne.getTauxApplique().multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP).toString() + " %";
            } else {
                tauxAffiche = "--";
            }
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
                .tauxAffiche(ligne.getTauxAffiche())
                .baseApplique(ligne.getBaseApplique())
                .formuleCalcul(ligne.getFormuleCalcul())
                .build();
    }


    public BulletinPaie mapCreateDtoToEntity(BulletinPaieCreateDto dto) {
        Employe employe = employeRepo.findById(dto.getEmployeId())
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID : " + dto.getEmployeId()));
        Entreprise entreprise = entrepriseRepository.findById(dto.getEntrepriseId())
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec l'ID : " + dto.getEntrepriseId()));

        BulletinPaie fiche = new BulletinPaie();
        fiche.setEmploye(employe);
        fiche.setEntreprise(entreprise);
        fiche.setHeuresSup(dto.getHeuresSup());
        fiche.setHeuresFerie(dto.getHeuresFerie());
        fiche.setHeuresNuit(dto.getHeuresNuit());
        fiche.setDatePaiement(dto.getDatePaiement());
        fiche.setMethodePaiement(dto.getMethodePaiement());
        // Ajoute ici les autres champs nécessaires si besoin

        return fiche;
    }

    @Transactional
    public BulletinPaieResponseDto calculateBulletin(BulletinPaieCreateDto dto) {
        BulletinPaie fiche = mapCreateDtoToEntity(dto);
        BulletinPaie calculBulletin = calculBulletin(fiche);
        initializeBulletinPaie(calculBulletin);
        return convertToDto(calculBulletin);
    }

    private void initializeBulletinPaie(BulletinPaie fiche) {
        Hibernate.initialize(fiche.getLignesPaie());
        for (LigneBulletinPaie ligne : fiche.getLignesPaie()) {
            Hibernate.initialize(ligne.getElementPaie());
        }
        Hibernate.initialize(fiche.getEmploye());
        if (fiche.getEmploye() != null) {
            Hibernate.initialize(fiche.getEmploye().getEntreprise());
        }
        Hibernate.initialize(fiche.getEntreprise());
    }

    //Methode Crud
    @Transactional
    public BulletinPaieResponseDto saveBulletinPaie(BulletinPaieCreateDto dto) {
        BulletinPaie fiche = mapCreateDtoToEntity(dto);
        BulletinPaie calculBulletin = calculBulletin(fiche);
        BulletinPaie saved = bulletinRepo.save(calculBulletin);
        initializeBulletinPaie(saved);
        auditLogService.logAction(
                "GENERATE_BULLETIN_PAIE",
                "BulletinPaie",
                fiche.getId(),
                auditLogService.getCurrentUsername(),
                "Génération du bulletin de paie pour l'employé "
                        + fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom() + "De l'entreprise " +fiche.getEntreprise().getNom()
        );
        return convertToDto(saved);
    }

    @Transactional
    public List<BulletinPaieResponseDto> getAllBulletinsPaie() {

        return bulletinRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    //pour gerer afficher en fonction  des role
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
    public Page<BulletinPaieEmployeurDto> getBulletinsForEmployerPaginated(
            int page, int size, String searchTerm, List<StatusBulletin> statuts
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));
        Entreprise entreprise = currentUser.getEntreprise();

        Pageable pageable = PageRequest.of(page, size);

        Page<BulletinPaie> bulletinsPage;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            bulletinsPage = bulletinRepo.findByEntrepriseAndStatusBulletinInAndEmploye_NomContainingIgnoreCaseOrEmploye_PrenomContainingIgnoreCaseOrEmploye_MatriculeContainingIgnoreCaseOrderByDateCreationBulletinDesc(
                    entreprise, statuts, searchTerm, searchTerm, searchTerm, pageable
            );
        } else {
            bulletinsPage = bulletinRepo.findByEntrepriseAndStatusBulletinInOrderByDateCreationBulletinDesc(entreprise, statuts, pageable);
        }

        return bulletinsPage.map(this::convertToEmployeurDto);
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
