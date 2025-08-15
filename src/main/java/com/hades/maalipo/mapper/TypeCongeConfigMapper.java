package com.hades.maalipo.mapper;

import com.hades.maalipo.dto.conge.TypeCongeConfigDTO;
import com.hades.maalipo.dto.conge.TypeCongeConfigResponseDto;
import com.hades.maalipo.model.TypeCongeConfig;
import org.springframework.stereotype.Component;

@Component
public class TypeCongeConfigMapper {

    public static TypeCongeConfigDTO toDTO(TypeCongeConfig config) {
        if (config == null) return null;
        TypeCongeConfigDTO dto = new TypeCongeConfigDTO();
        dto.setId(config.getId());
        dto.setEntrepriseId(config.getEntreprise() != null ? config.getEntreprise().getId() : null);
        dto.setTypeConge(config.getTypeConge());
        dto.setActif(config.getActif());
        dto.setDureeMaximaleJours(config.getDureeMaximaleJours());
        dto.setDelaiPreavisJours(config.getDelaiPreavisJours());
        dto.setPourcentageRemuneration(config.getPourcentageRemuneration());
        dto.setDocumentsRequis(config.getDocumentsRequis());
        dto.setConditionsAttribution(config.getConditionsAttribution());
        dto.setCumulAutorise(config.getCumulAutorise());
        return dto;
    }

    public static TypeCongeConfigResponseDto toResponseDto(TypeCongeConfig config) {
        if (config == null) {
            return null;
        }

        TypeCongeConfigResponseDto dto = new TypeCongeConfigResponseDto();

        // Mapping sécurisé
        dto.setId(config.getId());
        dto.setTypeConge(config.getTypeConge());
        dto.setDureeMaximaleJours(config.getDureeMaximaleJours());
        dto.setDelaiPreavisJours(config.getDelaiPreavisJours());
        dto.setPourcentageRemuneration(config.getPourcentageRemuneration());
        dto.setDocumentsRequis(config.getDocumentsRequis());
        dto.setConditionsAttribution(config.getConditionsAttribution());
        dto.setCumulAutorise(config.getCumulAutorise());
        dto.setActif(config.getActif());
        dto.setDocumentsObligatoires(config.getDocumentsObligatoires());

        // Mapping sécurisé de l'entreprise (évite les références circulaires)
        if (config.getEntreprise() != null) {
            dto.setEntrepriseId(config.getEntreprise().getId());
            dto.setEntrepriseNom(config.getEntreprise().getNom());
        }

        return dto;
    }
}