package com.hades.paie1.service;

import com.hades.paie1.enum1.CategorieElement;
import com.hades.paie1.enum1.FormuleCalculType;
import com.hades.paie1.enum1.TypeElementPaie;
import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.model.TemplateElementPaieConfig;
import com.hades.paie1.repository.BulletinTemplateRepository;
import com.hades.paie1.repository.ElementPaieRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ElementPaieInitializer {
    private final ElementPaieRepository elementPaieRepository;
    private static final Logger logger = LoggerFactory.getLogger(ElementPaieInitializer.class);
    private final BulletinTemplateRepository bulletinTemplateRepository;
    public ElementPaieInitializer(ElementPaieRepository elementPaieRepository,
                                  BulletinTemplateRepository bulletinTemplateRepository) {
        this.elementPaieRepository = elementPaieRepository;
        this.bulletinTemplateRepository = bulletinTemplateRepository;
    }

    @PostConstruct
    public void init() {
        createElements();
        createDefaultTemplateIfNotExists();
    }

    @Transactional
    public void createElements() {
        // Liste complète des éléments de paie du bulletin
        List<String> designations = Arrays.asList(
                "Salaire de base",
                "Prime de transport",
                "Prime de ponctualité",
                "Prime de technicité",
                "Prime d'ancienneté",
                "IRPP",
                "CAC",
                "Taxe communale",
                "Redevance audiovisuelle",
                "Crédit foncier salarial",
                "Crédit foncier patronal",
                "Fonds national de l'emploi",
                "Pension vieillesse CNPS",
                "Allocation familiale CNPS",
                "Accident de travail CNPS"

        );

        List<String> existingDesignations = elementPaieRepository.findByDesignationIn(designations)
                .stream()
                .map(ElementPaie::getDesignation)
                .collect(Collectors.toList());

        // === GAINS ===
        if (!existingDesignations.contains("Salaire de base")) {
            createElementPaie("Salaire de base", "100",
                    TypeElementPaie.GAIN, CategorieElement.SALAIRE_DE_BASE,
                    FormuleCalculType.NOMBRE_BASE_TAUX, null,
                    true,  // impacteSalaireBrut
                    true,  // impacteBaseCnps
                    true,  // impacteBaseIrpp
                    true,  // impacteSalaireBrutImposable
                    true,  // impacteBaseCreditFoncier
                    true,  // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Prime de transport")) {
            createElementPaie("Prime de transport", "107",
                    TypeElementPaie.GAIN, CategorieElement.PRIME,
                    FormuleCalculType.MONTANT_FIXE, null,
                    true,  // impacteSalaireBrut
                    false,  // impacteBaseCnps (incluse dans base CNPS)
                    true,  // impacteBaseIrpp (incluse dans base IRPP)
                    true,  // impacteSalaireBrutImposable
                    true,  // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete (transport n'entre pas dans base ancienneté)
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Prime de ponctualité")) {
            createElementPaie("Prime de ponctualité", "115",
                    TypeElementPaie.GAIN, CategorieElement.PRIME,
                    FormuleCalculType.MONTANT_FIXE, null,
                    true,  // impacteSalaireBrut
                    true,  // impacteBaseCnps
                    true,  // impacteBaseIrpp
                    true,  // impacteSalaireBrutImposable
                    true,  // impacteBaseCreditFoncier
                    true,  // impacteBaseAnciennete (prime régulière)
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Prime de technicité")) {
            createElementPaie("Prime de technicité", "117",
                    TypeElementPaie.GAIN, CategorieElement.PRIME,
                    FormuleCalculType.MONTANT_FIXE, null,
                    true,  // impacteSalaireBrut
                    true,  // impacteBaseCnps
                    true,  // impacteBaseIrpp
                    true,  // impacteSalaireBrutImposable
                    true,  // impacteBaseCreditFoncier
                    true,  // impacteBaseAnciennete (prime régulière)
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Prime d'ancienneté")) {
            createElementPaie("Prime d'ancienneté", "118",
                    TypeElementPaie.GAIN, CategorieElement.PRIME,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    true,  // impacteSalaireBrut
                    true,  // impacteBaseCnps
                    true,  // impacteBaseIrpp
                    true,  // impacteSalaireBrutImposable
                    true,  // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete (ne peut pas servir de base à elle-même)
                    true   // impacteNetAPayer
            );
        }

        // === RETENUES - IMPOTS ===
        if (!existingDesignations.contains("IRPP")) {
            createElementPaie("IRPP", "200",
                    TypeElementPaie.RETENUE, CategorieElement.IMPOT,
                    FormuleCalculType.BAREME, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("CAC")) {
            createElementPaie("CAC", "201",
                    TypeElementPaie.RETENUE, CategorieElement.IMPOT,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Taxe communale")) {
            createElementPaie("Taxe communale", "202",
                    TypeElementPaie.RETENUE, CategorieElement.IMPOT,
                    FormuleCalculType.BAREME, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Redevance audiovisuelle")) {
            createElementPaie("Redevance audiovisuelle", "203",
                    TypeElementPaie.RETENUE, CategorieElement.IMPOT,
                    FormuleCalculType.BAREME, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        // === RETENUES - COTISATIONS ===
        if (!existingDesignations.contains("Crédit foncier salarial")) {
            createElementPaie("Crédit foncier salarial", "204",
                    TypeElementPaie.RETENUE, CategorieElement.COTISATION_SALARIALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Fonds national de l'emploi")) {
            createElementPaie("Fonds national de l'emploi", "206",
                    TypeElementPaie.RETENUE, CategorieElement.COTISATION_SALARIALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }
        // === CHARGES PATRONALES ===
        if (!existingDesignations.contains("Crédit foncier patronal")) {
            createElementPaie("Crédit foncier patronal", "205",
                    TypeElementPaie.CHARGE_PATRONALE, CategorieElement.COTISATION_PATRONALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    false  // impacteNetAPayer (charge patronale)
            );
        }
        if (!existingDesignations.contains("Pension vieillesse CNPS")) {
            createElementPaie("Pension vieillesse CNPS", "CNPS_VIEILLESSE_PAT",
                    TypeElementPaie.CHARGE_PATRONALE, CategorieElement.COTISATION_PATRONALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }

        if (!existingDesignations.contains("Pension vieillesse CNPS")) {
            createElementPaie("Pension vieillesse CNPS", "CNPS_VIEILLESSE_SAL",
                    TypeElementPaie.RETENUE, CategorieElement.COTISATION_SALARIALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    true   // impacteNetAPayer
            );
        }





        if (!existingDesignations.contains("Allocation familiale CNPS")) {
            createElementPaie("Allocation familiale CNPS", "209",
                    TypeElementPaie.CHARGE_PATRONALE, CategorieElement.COTISATION_PATRONALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    false  // impacteNetAPayer (charge patronale)
            );
        }

        if (!existingDesignations.contains("Accident de travail CNPS")) {
            createElementPaie("Accident de travail CNPS", "210",
                    TypeElementPaie.CHARGE_PATRONALE, CategorieElement.COTISATION_PATRONALE,
                    FormuleCalculType.POURCENTAGE_BASE, null,
                    false, // impacteSalaireBrut
                    false, // impacteBaseCnps
                    false, // impacteBaseIrpp
                    false, // impacteSalaireBrutImposable
                    false, // impacteBaseCreditFoncier
                    false, // impacteBaseAnciennete
                    false  // impacteNetAPayer (charge patronale)
            );
        }

        logger.info("Tous les éléments de paie ont été initialisés avec succès");
    }

    private void createElementPaie(String designation, String code,
                                   TypeElementPaie type, CategorieElement categorie,
                                   FormuleCalculType formuleCalculType, java.math.BigDecimal tauxDefaut,
                                   boolean impacteSalaireBrut, boolean impacteBaseCnps,
                                   boolean impacteBaseIrpp, boolean impacteSalaireBrutImposable,
                                   boolean impacteBaseCreditFoncier, boolean impacteBaseAnciennete,
                                   boolean impacteNetAPayer) {
        try {
            ElementPaie element = ElementPaie.builder()
                    .designation(designation)
                    .code(code)
                    .type(type)
                    .categorie(categorie)
                    .formuleCalcul(formuleCalculType)
                    .tauxDefaut(tauxDefaut)
                    .impacteSalaireBrut(impacteSalaireBrut)
                    .impacteBaseCnps(impacteBaseCnps)
                    .impacteBaseIrpp(impacteBaseIrpp)
                    .impacteSalaireBrutImposable(impacteSalaireBrutImposable)
                    .impacteBaseCreditFoncier(impacteBaseCreditFoncier)
                    .impacteBaseAnciennete(impacteBaseAnciennete)
                    .impacteNetAPayer(impacteNetAPayer)
                    .build();

            elementPaieRepository.save(element);
            logger.info("Élément créé : {} (Code: {})", designation, code);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'élément {}: {}", designation, e.getMessage());
        }
    }

    private void createDefaultTemplateIfNotExists(){

        boolean exists = bulletinTemplateRepository.findAll().stream()
                .anyMatch(t -> t.isDefault() && t.getEntreprise() == null);
        if (exists) {
            logger.info("Le template par défaut existe déjà.");
            return;
        }
        BulletinTemplate defaultTemplate = new BulletinTemplate();
        defaultTemplate.setNom("Template par défaut");
        defaultTemplate.setDefault(true);
        defaultTemplate.setEntreprise(null); // Non associé à une entreprise

        List<ElementPaie> allElements = elementPaieRepository.findAll();
        int ordre = 0;
        for (ElementPaie ep : allElements) {
            TemplateElementPaieConfig config = TemplateElementPaieConfig.builder()
                    .bulletinTemplate(defaultTemplate)
                    .elementPaie(ep)
                    .isActive(true)
                    .formuleCalculOverride(ep.getFormuleCalcul())
                    .tauxDefaut(ep.getTauxDefaut())
                    .montantDefaut(ep.getMontantDefaut())
                    .nombreDefaut(ep.getNombreDefaut())
                    .affichageOrdre(ordre++)
                    .build();
            defaultTemplate.getElementsConfig().add(config);
        }
        bulletinTemplateRepository.save(defaultTemplate);
        logger.info("Le template par défaut a été créé avec succès.");
    }
}


