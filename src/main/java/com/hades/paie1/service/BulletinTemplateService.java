package com.hades.paie1.service;

import com.hades.paie1.dto.*; // Assurez-vous que tous les DTOs nécessaires sont importés
import com.hades.paie1.enum1.FormuleCalculType; // Import de l'enum
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.TemplateElementPaieConfig;
import com.hades.paie1.repository.BulletinTemplateRepository;
import com.hades.paie1.repository.ElementPaieRepository;
import com.hades.paie1.repository.EntrepriseRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BulletinTemplateService {

    private final BulletinTemplateRepository bulletinTemplateRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ElementPaieRepository elementPaieRepository;

    private static final Logger logger = LoggerFactory.getLogger(BulletinPaieService.class);

    public BulletinTemplateService(BulletinTemplateRepository bulletinTemplateRepository,
                                   EntrepriseRepository entrepriseRepository,
                                   ElementPaieRepository elementPaieRepository) {
        this.bulletinTemplateRepository = bulletinTemplateRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.elementPaieRepository = elementPaieRepository;
    }

    // --- Méthodes de conversion Entité -> DTO ---
    private BulletinTemplateDto convertToDto(BulletinTemplate template) {
        BulletinTemplateDto dto = new BulletinTemplateDto();
        dto.setId(template.getId());
        dto.setNom(template.getNom());
        dto.setDefault(template.isDefault());

        // Convertir Entreprise
        if (template.getEntreprise() != null) {
            EntrepriseDto entrepriseDto = new EntrepriseDto();
            entrepriseDto.setId(template.getEntreprise().getId());
            entrepriseDto.setNom(template.getEntreprise().getNom());
            dto.setEntreprise(entrepriseDto);
        }


        // Convertir la liste de TemplateElementPaieConfig
        if (template.getElementsConfig() != null) {
            dto.setElementsConfig(template.getElementsConfig().stream()
                    .map(this::convertConfigToDto) // Appel à la méthode de conversion de config
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // Méthode de conversion spécifique pour TemplateElementPaieConfig Entité -> DTO
    private TemplateElementPaieConfigDto convertConfigToDto(TemplateElementPaieConfig config) {


        TemplateElementPaieConfigDto dto = new TemplateElementPaieConfigDto();
        dto.setId(config.getId());
        dto.setActive(config.isActive());
        dto.setTauxDefaut(config.getTauxDefaut());
        dto.setNombreDefaut(config.getNombreDefaut());
        dto.setMontantDefaut(config.getMontantDefaut());
        dto.setFormuleCalculOverride(config.getFormuleCalculOverride());
        dto.setAffichageOrdre(config.getAffichageOrdre());


        // Convertir ElementPaie
        if (config.getElementPaie() != null) {
            ElementPaieDto elementPaieDto = new ElementPaieDto();
            ElementPaie ep = config.getElementPaie();
            elementPaieDto.setId(ep.getId());
            elementPaieDto.setCode(ep.getCode());
            elementPaieDto.setType(ep.getType());
            elementPaieDto.setFormuleCalcul(ep.getFormuleCalcul());
            elementPaieDto.setTauxDefaut(ep.getTauxDefaut());
            elementPaieDto.setNombreDefaut(ep.getNombreDefaut());
            elementPaieDto.setMontantDefaut(ep.getMontantDefaut());
//            elementPaieDto.setUniteBaseCalcul(ep.getUniteBaseCalcul());
            elementPaieDto.setCategorie(ep.getCategorie());
            elementPaieDto.setDesignation(ep.getDesignation());
            elementPaieDto.setImpacteSalaireBrut(ep.isImpacteSalaireBrut());
            elementPaieDto.setImpacteBaseCnps(ep.isImpacteBaseCnps());
            elementPaieDto.setImpacteBaseIrpp(ep.isImpacteBaseIrpp());
            elementPaieDto.setImpacteSalaireBrutImposable(ep.isImpacteSalaireBrutImposable());
            elementPaieDto.setImpacteBaseCreditFoncier(ep.isImpacteBaseCreditFoncier());
            elementPaieDto.setImpacteBaseAnciennete(ep.isImpacteBaseAnciennete());
            elementPaieDto.setImpacteNetAPayer(ep.isImpacteNetAPayer());

            dto.setElementPaieId(config.getElementPaie().getId());

        }
        return dto;
    }

    // --- Méthodes de conversion DTO -> Entité (NOUVELLES) ---
    private BulletinTemplate convertToEntity(BulletinTemplateDto dto) {
        BulletinTemplate entity = new BulletinTemplate();
        // ID est généralement défini lors de la création ou par le chemin pour la mise à jour
        // entity.setId(dto.getId()); // Pour les mises à jour, l'ID est dans le chemin
        entity.setNom(dto.getNom());
        entity.setDefault(dto.isDefault());


        return entity;
    }

    private TemplateElementPaieConfig convertConfigToEntity(TemplateElementPaieConfigDto dto) {
        TemplateElementPaieConfig entity = new TemplateElementPaieConfig();

        entity.setId(dto.getId());

        entity.setActive(dto.isActive());
        entity.setTauxDefaut(dto.getTauxDefaut());
        entity.setNombreDefaut(dto.getNombreDefaut());
        entity.setMontantDefaut(dto.getMontantDefaut());
        entity.setFormuleCalculOverride(dto.getFormuleCalculOverride());
        entity.setAffichageOrdre(dto.getAffichageOrdre());
        return entity;
    }



    @Transactional
    public List<BulletinTemplateDto> getTemplatesByEntreprise(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec id : " + entrepriseId));
        List<BulletinTemplate> templates = bulletinTemplateRepository.findByEntreprise(entreprise);
        return templates.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public Optional<BulletinTemplateDto> getBulletinTemplateById(Long id) {
        return bulletinTemplateRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public BulletinTemplateDto createBulletinTemplate(Long entrepriseId, BulletinTemplateDto newTemplateDto) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec id : " + entrepriseId));

        BulletinTemplate newTemplate = convertToEntity(newTemplateDto); // Convertir DTO en Entité
        newTemplate.setEntreprise(entreprise);

        if (newTemplate.isDefault()) { // La logique pour isDefault est gérée ici
            entreprise.getBulletinTemplates().stream()
                    .filter(BulletinTemplate::isDefault)
                    .findFirst()
                    .ifPresent(oldDefault -> {
                        oldDefault.setDefault(false);
                        bulletinTemplateRepository.save(oldDefault);
                    });
        }

        // Gérer les elementsConfig (s'ils sont inclus dans le DTO de création)
        if (newTemplateDto.getElementsConfig() != null && !newTemplateDto.getElementsConfig().isEmpty()) {
            for (TemplateElementPaieConfigDto configDto : newTemplateDto.getElementsConfig()) {
                TemplateElementPaieConfig config = convertConfigToEntity(configDto);
                config.setBulletinTemplate(newTemplate);
                // Utiliser elementPaieId au lieu de elementPaie.getId()
                ElementPaie ep = elementPaieRepository.findById(configDto.getElementPaieId())
                        .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id : " + configDto.getElementPaieId()));
                config.setElementPaie(ep);
                newTemplate.getElementsConfig().add(config);
            }
        }

        BulletinTemplate createdTemplate = bulletinTemplateRepository.save(newTemplate);
        return convertToDto(createdTemplate);
    }

    @Transactional
    public BulletinTemplateDto updateBulletinTemplate(Long id, BulletinTemplateDto updatedTemplateDto) {
        BulletinTemplate existingTemplate = bulletinTemplateRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("BulletinTemplate non trouvé avec id : " + id));

        existingTemplate.setNom(updatedTemplateDto.getNom());
//        // Mettre à jour HeuresConfig
//        if (updatedTemplateDto.getHeuresConfig() != null) {
//            BulletinTemplate.HeuresConfig heuresConfigEntity = existingTemplate.getHeuresConfig();
//            if (heuresConfigEntity == null) {
//                heuresConfigEntity = new BulletinTemplate.HeuresConfig();
//                existingTemplate.setHeuresConfig(heuresConfigEntity);
//            }
//            heuresConfigEntity.setHeuresNormalesActive(updatedTemplateDto.getHeuresConfig().isHeuresNormalesActive());
//            heuresConfigEntity.setHeuresSup1Active(updatedTemplateDto.getHeuresConfig().isHeuresSup1Active());
//            heuresConfigEntity.setTauxSup1(updatedTemplateDto.getHeuresConfig().getTauxSup1());
//        } else {
//            existingTemplate.setHeuresConfig(null); // Si le DTO envoie null, supprimez l'intégration
//        }


        // Gérer le drapeau isDefault et la relation dans Entreprise
        if (updatedTemplateDto.isDefault() && !existingTemplate.isDefault()) {
            Entreprise entreprise = existingTemplate.getEntreprise();
            if (entreprise != null) {
                entreprise.getBulletinTemplates().stream()
                        .filter(t -> t.isDefault() && !t.getId().equals(existingTemplate.getId()))
                        .findFirst()
                        .ifPresent(oldDefault -> {
                            oldDefault.setDefault(false);
                            bulletinTemplateRepository.save(oldDefault);
                        });
            }
        } else if (!updatedTemplateDto.isDefault() && existingTemplate.isDefault()) {
            // Si ce template n'est plus par défaut et qu'il était le defaultTemplate de l'entreprise
            Entreprise entreprise = existingTemplate.getEntreprise();
            if (entreprise != null && entreprise.getDefaultBulletinTemplate() != null &&
                    entreprise.getDefaultBulletinTemplate().getId().equals(existingTemplate.getId())) {
                // entreprise.setDefaultBulletinTemplate(null); // Attention: cette ligne pourrait être problématique si pas @OneToOne
                // Il vaut mieux gérer isDefault par la liste directement ou revoir la relation par défaut si elle est @OneToOne
                // Pour l'approche courante (isDefault flag dans l'entité), la logique ci-dessus suffit.
            }
        }
        existingTemplate.setDefault(updatedTemplateDto.isDefault());

        // Gérer les elementsConfig (diff logic)
        if (updatedTemplateDto.getElementsConfig() != null) {
            // Créer une map des configurations existantes pour un accès rapide
            var existingConfigsMap = existingTemplate.getElementsConfig().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            config -> config.getElementPaie().getId(),
                            config -> config
                    ));

            List<TemplateElementPaieConfig> newTemplateElements = new java.util.ArrayList<>();

            for (TemplateElementPaieConfigDto updatedConfigDto : updatedTemplateDto.getElementsConfig()) {
                // L'ElementPaie doit toujours exister
                ElementPaie elementPaie = elementPaieRepository.findById(updatedConfigDto.getElementPaieId())
                        .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id : " + updatedConfigDto.getElementPaieId()));

                TemplateElementPaieConfig existingConfig = existingConfigsMap.get(elementPaie.getId());

                if (existingConfig != null) {
                    // Mettre à jour la configuration existante
                    existingConfig.setActive(updatedConfigDto.isActive());
                    existingConfig.setTauxDefaut(updatedConfigDto.getTauxDefaut());
                    existingConfig.setMontantDefaut(updatedConfigDto.getMontantDefaut());
                    existingConfig.setNombreDefaut(updatedConfigDto.getNombreDefaut());
                    existingConfig.setFormuleCalculOverride(updatedConfigDto.getFormuleCalculOverride());
                    existingConfig.setAffichageOrdre(updatedConfigDto.getAffichageOrdre());
                    newTemplateElements.add(existingConfig);
                    existingConfigsMap.remove(elementPaie.getId());
                } else {
                    // Créer une nouvelle configuration
                    TemplateElementPaieConfig newConfig = convertConfigToEntity(updatedConfigDto);
                    newConfig.setBulletinTemplate(existingTemplate);
                    newConfig.setElementPaie(elementPaie);
                    newTemplateElements.add(newConfig);
                }
            }

            // Supprimer les configurations qui ne sont plus dans le DTO
            existingTemplate.getElementsConfig().removeIf(config -> existingConfigsMap.containsValue(config)); // Utilise les valeurs restantes dans la map

            // Ajouter/Mettre à jour la liste des configurations
            existingTemplate.getElementsConfig().clear(); // Effacer toutes les anciennes pour ajouter les nouvelles/mises à jour
            existingTemplate.getElementsConfig().addAll(newTemplateElements);

        } else {
            // Si elementsConfig est null dans le DTO, cela signifie que toutes les configurations doivent être supprimées
            existingTemplate.getElementsConfig().clear();
        }

        BulletinTemplate savedTemplate = bulletinTemplateRepository.save(existingTemplate);
        return convertToDto(savedTemplate);
    }

    public void deleteBulletinTemplate(Long id) {
        BulletinTemplate templateToDelete = bulletinTemplateRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("BulletinTemplate non trouvé avec id : " + id));

        templateToDelete.getElementsConfig().clear();
        bulletinTemplateRepository.save(templateToDelete);
        bulletinTemplateRepository.delete(templateToDelete);
    }

    @Transactional
    public TemplateElementPaieConfigDto addOrUpdateElementToTemplate(
            Long templateId, TemplateElementPaieConfigDto configDto) {

        BulletinTemplate template = bulletinTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RessourceNotFoundException("Template non trouvé avec id : " + templateId));

        ElementPaie elementPaie = elementPaieRepository.findById(configDto.getElementPaieId())
                .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id : " + configDto.getElementPaieId()));

        // Vérifier que l'ElementPaie appartient à l'entreprise du template ou est global
//        if (elementPaie.get != null && !elementPaie.getEntreprise().getId().equals(template.getEntreprise().getId())) {
//            throw new IllegalArgumentException("L'ElementPaie ne peut pas être ajouté à ce template car il appartient à une autre entreprise.");
//        }

        if (configDto.getFormuleCalculOverride() == null) {
            configDto.setFormuleCalculOverride(elementPaie.getFormuleCalcul());
        }
        Optional<TemplateElementPaieConfig> existingConfigOpt = template.getElementsConfig().stream()
                .filter(config -> config.getElementPaie().getId().equals(elementPaie.getId()))
                .findFirst();

        TemplateElementPaieConfig config;
        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
            // Mise à jour des champs existants
            config.setFormuleCalculOverride(configDto.getFormuleCalculOverride());
            config.setMontantDefaut(configDto.getMontantDefaut());
            config.setTauxDefaut(configDto.getTauxDefaut());
            config.setNombreDefaut(configDto.getNombreDefaut());
            config.setActive(configDto.isActive());
            config.setFormuleCalculOverride(configDto.getFormuleCalculOverride());
            config.setAffichageOrdre(configDto.getAffichageOrdre()); // <--- AJOUT CLÉ ICI
        } else {
            // Création d'une nouvelle configuration
            config = TemplateElementPaieConfig.builder()
                    .bulletinTemplate(template)
                    .elementPaie(elementPaie)
                    .formuleCalculOverride(configDto.getFormuleCalculOverride())
                    .tauxDefaut(configDto.getTauxDefaut())
                    .montantDefaut(configDto.getMontantDefaut())
                    .nombreDefaut(configDto.getNombreDefaut())
                    .isActive(configDto.isActive())
                    .affichageOrdre(configDto.getAffichageOrdre()) // <--- ET ICI
                    .build();
            template.getElementsConfig().add(config);
        }

        // Sauvegarde du template pour persister les changements de la collection
        // Note: Si cascade = CascadeType.ALL est configuré sur elementsConfig, la sauvegarde du template suffira.
        bulletinTemplateRepository.save(template);

        return convertToDto(config);
    }
    @Transactional
    public void removeElementFromTemplate(Long templateId, Long elementPaieId) {
        BulletinTemplate template = bulletinTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RessourceNotFoundException("BulletinTemplate non trouvé avec id : " + templateId));

        boolean removed = template.getElementsConfig()
                .removeIf(config -> config.getElementPaie().getId().equals(elementPaieId));

        if (!removed) {
            throw new RessourceNotFoundException("L'ElementPaie avec id " + elementPaieId + " n'est pas configuré dans le template " + templateId);
        }
        bulletinTemplateRepository.save(template); // Sauvegarde pour appliquer la suppression en cascade
    }

    @Transactional
    public void setDefaultTemplate(Long templateId) {
        BulletinTemplate template = bulletinTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RessourceNotFoundException("Template non trouvé avec id : " + templateId));
        Entreprise entreprise = template.getEntreprise();
        if (entreprise == null) throw new RessourceNotFoundException("Entreprise non trouvée");

        // Mettre à false tous les autres templates de cette entreprise
        for (BulletinTemplate t : entreprise.getBulletinTemplates()) {
            if (t.isDefault() && !t.getId().equals(templateId)) {
                t.setDefault(false);
                bulletinTemplateRepository.save(t);
            }
        }
        template.setDefault(true);
        bulletinTemplateRepository.save(template);

        // Mettre à jour la colonne "defaultBulletinTemplate" dans l'entreprise
        entreprise.setDefaultBulletinTemplate(template);
        entrepriseRepository.save(entreprise);
    }

    private TemplateElementPaieConfigDto convertToDto(TemplateElementPaieConfig config) {
        TemplateElementPaieConfigDto dto = new TemplateElementPaieConfigDto();
        dto.setId(config.getId());
        dto.setElementPaieId(config.getElementPaie().getId());
        dto.setFormuleCalculOverride(config.getFormuleCalculOverride());
        dto.setTauxDefaut(config.getTauxDefaut());
        dto.setMontantDefaut(config.getMontantDefaut());
        dto.setNombreDefaut(config.getNombreDefaut());
        dto.setActive(config.isActive());
        if (config.getFormuleCalculOverride() == null) {
            dto.setFormuleCalculOverride(config.getElementPaie().getFormuleCalcul());
        } else {
            dto.setFormuleCalculOverride(config.getFormuleCalculOverride());
        }
        dto.setAffichageOrdre(config.getAffichageOrdre()); // <--- ASSUREZ-VOUS QUE C'EST BIEN MAPPÉ ICI AUSSI
        return dto;
    }

}