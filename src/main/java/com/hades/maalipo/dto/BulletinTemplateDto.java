package com.hades.maalipo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BulletinTemplateDto {
    private Long id;
    private String nom;

    @JsonProperty("isDefault")
    private boolean isDefault;

    private EntrepriseDto entreprise;
    private HeuresConfigDto heuresConfig;
    private List<TemplateElementPaieConfigDto> elementsConfig;
}