package com.hades.maalipo.dto.bulletin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hades.maalipo.dto.elementpaie.HeuresConfigDto;
import com.hades.maalipo.dto.elementpaie.TemplateElementPaieConfigDto;
import com.hades.maalipo.dto.entreprise.EntrepriseDto;
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