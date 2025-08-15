package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.TypeCongeConfigDTO;
import com.hades.maalipo.dto.conge.TypeCongeConfigResponseDto;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.mapper.TypeCongeConfigMapper;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.TypeCongeConfig;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.repository.TypeCongeConfigRepository;
import com.hades.maalipo.service.AuditLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TypeCongeConfigService {

    private final TypeCongeConfigRepository typeCongeConfigRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final AuditLogService auditLogService;


    public TypeCongeConfigService(TypeCongeConfigRepository typeCongeConfigRepository,
                                  EntrepriseRepository entrepriseRepository,
                                  AuditLogService auditLogService) {
        this.typeCongeConfigRepository = typeCongeConfigRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void initializeDefaultConfigsForEntreprise(Long entrepriseId) {
        Optional<Entreprise> entreprise = entrepriseRepository.findById(entrepriseId);
        if (entreprise.isEmpty()) {
            throw new IllegalArgumentException("Entreprise non trouvée: " + entrepriseId);
        }

        for (TypeConge typeConge : TypeConge.values()) {
            if (!configExists(entrepriseId, typeConge)) {
                TypeCongeConfig config = new TypeCongeConfig(entreprise.get(), typeConge);
                setDefaultDocumentsRequis(config, typeConge);
                typeCongeConfigRepository.save(config);
            }
        }
    }

    private void setDefaultDocumentsRequis(TypeCongeConfig config, TypeConge typeConge) {
        switch (typeConge) {
            case CONGE_MALADIE:
            case CONGE_ACCIDENT_TRAVAIL:
                config.setDocumentsRequis("CERTIFICAT_MEDICAL");
                config.setDocumentsObligatoires(true);
                break;
            case CONGE_MARIAGE:
                config.setDocumentsRequis("ACTE_MARIAGE,CERTIFICAT_CELIBAT");
                config.setDocumentsObligatoires(true);
                break;
            case CONGE_DEUIL:
                config.setDocumentsRequis("ACTE_DECES,LIEN_FAMILLE");
                config.setDocumentsObligatoires(false);
                break;
            case CONGE_MATERNITE:
                config.setDocumentsRequis("CERTIFICAT_GROSSESSE,CERTIFICAT_ACCOUCHEMENT");
                config.setDocumentsObligatoires(true);
                break;
            case CONGE_PATERNITE:
                config.setDocumentsRequis("ACTE_NAISSANCE");
                config.setDocumentsObligatoires(true);
                break;
            case CONGE_PAYE:  // Ajoutez cette clause explicitement
                config.setDocumentsRequis("");
                config.setDocumentsObligatoires(false);
                break;
            default:
                config.setDocumentsObligatoires(false);
        }
    }

    public boolean configExists(Long entrepriseId, TypeConge typeConge) {
        return typeCongeConfigRepository
                .findByEntrepriseIdAndTypeConge(entrepriseId, typeConge)
                .isPresent();
    }

    public List<TypeCongeConfigDTO> getAllConfigsByEntreprise(Long entrepriseId) {
        List<TypeCongeConfig> configs = typeCongeConfigRepository
                .findAllConfigsByEntreprise(entrepriseId); // ✅ NOUVELLE MÉTHODE

        return configs.stream()
                .map(TypeCongeConfigMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TypeCongeConfigDTO> getActiveConfigsByEntreprise(Long entrepriseId) {
        List<TypeCongeConfig> configs = typeCongeConfigRepository.findAllActiveConfigsByEntreprise(entrepriseId);
        return configs.stream()
                .map(TypeCongeConfigMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<TypeCongeConfigDTO> getActiveConfigByEntrepriseAndType(Long entrepriseId, TypeConge typeConge) {
        return typeCongeConfigRepository.findActiveConfigByEntrepriseAndType(entrepriseId, typeConge)
                .map(TypeCongeConfigMapper::toDTO);
    }

    public Optional<TypeCongeConfigDTO> getConfigByEntrepriseAndType(Long entrepriseId, TypeConge typeConge) {
        return getActiveConfigByEntrepriseAndType(entrepriseId, typeConge);
    }

    @Transactional
    public TypeCongeConfig updateConfig(Long configId, TypeCongeConfig updates, Long entrepriseId) {
        TypeCongeConfig existing = typeCongeConfigRepository.findById(configId)
                .orElseThrow(() -> new RessourceNotFoundException("Configuration non trouvée: " + configId));

        // Vérification de sécurité : s'assurer que la configuration appartient à l'entreprise
        if (!existing.getEntreprise().getId().equals(entrepriseId)) {
            throw new AccessDeniedException("Accès refusé : cette configuration n'appartient pas à votre entreprise");
        }

        if (updates.getDureeMaximaleJours() != null) {
            existing.setDureeMaximaleJours(updates.getDureeMaximaleJours());
        }
        if (updates.getDelaiPreavisJours() != null) {
            existing.setDelaiPreavisJours(updates.getDelaiPreavisJours());
        }
        if (updates.getPourcentageRemuneration() != null) {
            existing.setPourcentageRemuneration(updates.getPourcentageRemuneration());
        }
        if (updates.getDocumentsRequis() != null) {
            existing.setDocumentsRequis(updates.getDocumentsRequis());
        }
        if (updates.getConditionsAttribution() != null) {
            existing.setConditionsAttribution(updates.getConditionsAttribution());
        }
        if (updates.getCumulAutorise() != null) {
            existing.setCumulAutorise(updates.getCumulAutorise());
        }
        if (updates.getActif() != null) {
            existing.setActif(updates.getActif());
        }

        TypeCongeConfig savedConfig = typeCongeConfigRepository.save(existing);

        auditLogService.logAction(
                "UPDATE_TYPE_CONGE_CONFIG",
                "TypeCongeConfig",
                savedConfig.getId(),
                auditLogService.getCurrentUsername(),
                String.format("Configuration modifiée pour %s dans l'entreprise %d",
                        savedConfig.getTypeConge(),
                        entrepriseId)
        );

        return savedConfig;
    }

    // Méthode pour basculer le statut actif
    @Transactional
    public TypeCongeConfigResponseDto toggleActiveStatus(Long configId, Long entrepriseId) {
        TypeCongeConfig existing = typeCongeConfigRepository.findById(configId)
                .orElseThrow(() -> new RessourceNotFoundException("Configuration non trouvée: " + configId));

        // Vérification de sécurité
        if (!existing.getEntreprise().getId().equals(entrepriseId)) {
            throw new AccessDeniedException("Accès refusé : cette configuration n'appartient pas à votre entreprise");
        }

        existing.setActif(!existing.getActif());
        TypeCongeConfig saved = typeCongeConfigRepository.save(existing);

        // Force la synchronisation avec la base de données
        typeCongeConfigRepository.flush();


        auditLogService.logAction(
                "TOGGLE_TYPE_CONGE_CONFIG",
                "TypeCongeConfig",
                saved.getId(),
                auditLogService.getCurrentUsername(),
                String.format("Statut changé: %s %s pour l'entreprise %d",
                        saved.getTypeConge(),
                        saved.getActif() ? "ACTIVÉ" : "DÉSACTIVÉ",
                        entrepriseId)
        );
        // Conversion en DTO pour éviter les problèmes de proxy Hibernate
        return TypeCongeConfigMapper.toResponseDto(saved);
    }


}