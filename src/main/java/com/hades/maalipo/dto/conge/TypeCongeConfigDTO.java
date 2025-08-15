package com.hades.maalipo.dto.conge;

import com.hades.maalipo.enum1.TypeConge;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TypeCongeConfigDTO {
    private Long id;
    private Long entrepriseId;
    private TypeConge typeConge;
    private Boolean actif;

    @Min(value = 0, message = "La durée maximale ne peut pas être négative")
    private Integer dureeMaximaleJours;
    @Min(value = 0, message = "Le délai de préavis ne peut pas être négatif")
    private Integer delaiPreavisJours;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    private BigDecimal pourcentageRemuneration;

    private String documentsRequis;
    private String conditionsAttribution;
    private Boolean cumulAutorise;
    private Boolean documentsObligatoires;
}