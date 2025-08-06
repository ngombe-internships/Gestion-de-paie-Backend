package com.hades.paie1.service;

import com.hades.paie1.dto.ElementPaieDto;
import com.hades.paie1.dto.TemplateElementPaieConfigDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.model.TemplateElementPaieConfig;
import com.hades.paie1.repository.BulletinTemplateRepository;
import com.hades.paie1.repository.ElementPaieRepository;
import com.hades.paie1.repository.TemplateElementPaieConfigRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TemplateElementPaieConfigService {

    private final TemplateElementPaieConfigRepository templateElementPaieConfigRepository;
    private final BulletinTemplateRepository bulletinTemplateRepository;
    private final ElementPaieRepository elementPaieRepository;

    public TemplateElementPaieConfigService(
            TemplateElementPaieConfigRepository templateElementPaieConfigRepository,
            BulletinTemplateRepository bulletinTemplateRepository,
            ElementPaieRepository elementPaieRepository) {
        this.templateElementPaieConfigRepository = templateElementPaieConfigRepository;
        this.bulletinTemplateRepository = bulletinTemplateRepository;
        this.elementPaieRepository = elementPaieRepository;
    }

    // Méthode de conversion Entité -> DTO
    private TemplateElementPaieConfigDto convertToDto(TemplateElementPaieConfig entity) {
        TemplateElementPaieConfigDto dto = new TemplateElementPaieConfigDto();
        dto.setId(entity.getId());
        dto.setActive(entity.isActive());
        dto.setTauxDefaut(entity.getTauxDefaut());
        dto.setMontantDefaut(entity.getMontantDefaut());
        dto.setNombreDefaut(entity.getNombreDefaut());
        dto.setFormuleCalculOverride(entity.getFormuleCalculOverride()); // Utilisation directe de l'enum
        dto.setAffichageOrdre(entity.getAffichageOrdre());

        if (entity.getElementPaie() != null) {
            ElementPaieDto elementPaieDto = new ElementPaieDto();
            ElementPaie elementPaieEntity = entity.getElementPaie();
            elementPaieDto.setId(elementPaieEntity.getId());
            elementPaieDto.setCode(elementPaieEntity.getCode());
            elementPaieDto.setType(elementPaieEntity.getType());
            elementPaieDto.setFormuleCalcul(elementPaieEntity.getFormuleCalcul());
            elementPaieDto.setTauxDefaut(elementPaieEntity.getTauxDefaut());
            elementPaieDto.setNombreDefaut(elementPaieEntity.getNombreDefaut());
            elementPaieDto.setMontantDefaut(elementPaieEntity.getMontantDefaut());
//            elementPaieDto.setUniteBaseCalcul(elementPaieEntity.getUniteBaseCalcul());
            elementPaieDto.setCategorie(elementPaieEntity.getCategorie());
            elementPaieDto.setDesignation(elementPaieEntity.getDesignation());
            elementPaieDto.setImpacteSalaireBrut(elementPaieEntity.isImpacteSalaireBrut());
            elementPaieDto.setImpacteBaseCnps(elementPaieEntity.isImpacteBaseCnps());
            elementPaieDto.setImpacteBaseIrpp(elementPaieEntity.isImpacteBaseIrpp());
            elementPaieDto.setImpacteSalaireBrutImposable(elementPaieEntity.isImpacteSalaireBrutImposable());
            elementPaieDto.setImpacteBaseCreditFoncier(elementPaieEntity.isImpacteBaseCreditFoncier());
            elementPaieDto.setImpacteBaseAnciennete(elementPaieEntity.isImpacteBaseAnciennete());
            elementPaieDto.setImpacteNetAPayer(elementPaieEntity.isImpacteNetAPayer());


            dto.setElementPaieId(entity.getElementPaie().getId());
        }
        System.out.println("DTO mapping nombreDefaut: " + entity.getNombreDefaut());
        return dto;
    }

    // Méthode de conversion DTO -> Entité (pour la création/mise à jour)
    // Note: Le DTO ne contient pas bulletinTemplate. On le gère par les paramètres de la méthode.
    private TemplateElementPaieConfig convertToEntity(TemplateElementPaieConfigDto dto) {
        TemplateElementPaieConfig entity = new TemplateElementPaieConfig();
        entity.setId(dto.getId()); // Pour les mises à jour
        entity.setActive(dto.isActive());
        entity.setTauxDefaut(dto.getTauxDefaut());
        entity.setMontantDefaut(dto.getMontantDefaut());
        entity.setNombreDefaut(dto.getNombreDefaut());
        entity.setFormuleCalculOverride(dto.getFormuleCalculOverride());
        entity.setAffichageOrdre(dto.getAffichageOrdre());
        // elementPaie et bulletinTemplate seront définis dans les méthodes create/update

        System.out.println("DTO mapping nombreDefaut: " + entity.getNombreDefaut());
        return entity;
    }

    @Transactional
    public List<TemplateElementPaieConfigDto> getAllTemplateElementPaieConfigs() {
        return templateElementPaieConfigRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<TemplateElementPaieConfigDto> getTemplateElementPaieConfigById(Long id) {
        return templateElementPaieConfigRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public List<TemplateElementPaieConfigDto> getTemplateElementPaieConfigsByBulletinTemplateId(Long templateId) {
        BulletinTemplate bulletinTemplate = bulletinTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RessourceNotFoundException("BulletinTemplate non trouvé avec id : " + templateId));
        return templateElementPaieConfigRepository.findByBulletinTemplate(bulletinTemplate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TemplateElementPaieConfigDto createTemplateElementPaieConfig(
            Long bulletinTemplateId, Long elementPaieId, TemplateElementPaieConfigDto configDto) {
        if (configDto.getTauxDefaut() != null && configDto.getTauxDefaut().compareTo(BigDecimal.ONE) > 0) {
            configDto.setTauxDefaut(configDto.getTauxDefaut().divide(BigDecimal.valueOf(100)));
        }

        BulletinTemplate bulletinTemplate = bulletinTemplateRepository.findById(bulletinTemplateId)
                .orElseThrow(() -> new RessourceNotFoundException("BulletinTemplate non trouvé avec id : " + bulletinTemplateId));
        ElementPaie elementPaie = elementPaieRepository.findById(elementPaieId)
                .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id : " + elementPaieId));

        TemplateElementPaieConfig newConfig = convertToEntity(configDto);
        newConfig.setBulletinTemplate(bulletinTemplate);
        newConfig.setElementPaie(elementPaie);

        TemplateElementPaieConfig savedConfig = templateElementPaieConfigRepository.save(newConfig);
        return convertToDto(savedConfig);
    }

    @Transactional
    public TemplateElementPaieConfigDto updateTemplateElementPaieConfig(Long id, TemplateElementPaieConfigDto configDto) {
        if (configDto.getTauxDefaut() != null && configDto.getTauxDefaut().compareTo(BigDecimal.ONE) > 0) {
            configDto.setTauxDefaut(configDto.getTauxDefaut().divide(BigDecimal.valueOf(100)));
        }

        TemplateElementPaieConfig existingConfig = templateElementPaieConfigRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("TemplateElementPaieConfig non trouvé avec id : " + id));

        // Mettre à jour les champs
        existingConfig.setActive(configDto.isActive());
        existingConfig.setTauxDefaut(configDto.getTauxDefaut());
        existingConfig.setMontantDefaut(configDto.getMontantDefaut());

        existingConfig.setNombreDefaut(configDto.getNombreDefaut());
        existingConfig.setFormuleCalculOverride(configDto.getFormuleCalculOverride());
        existingConfig.setAffichageOrdre(configDto.getAffichageOrdre());

        // Si ElementPaie change, il faudrait le gérer. Pour l'instant, on suppose que ElementPaie n'est pas modifiable pour une config existante via cette méthode.
        // Si vous voulez permettre de changer l'ElementPaie associé, vous devrez ajouter un ID dans le DTO et le rechercher/setter ici.

        TemplateElementPaieConfig updatedConfig = templateElementPaieConfigRepository.save(existingConfig);
        return convertToDto(updatedConfig);
    }

    public void deleteTemplateElementPaieConfig(Long id) {
        if (!templateElementPaieConfigRepository.existsById(id)) {
            throw new RessourceNotFoundException("TemplateElementPaieConfig non trouvé avec id : " + id);
        }
        templateElementPaieConfigRepository.deleteById(id);
    }
}
