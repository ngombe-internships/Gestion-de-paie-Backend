package com.hades.paie1.dto;

import com.hades.paie1.enum1.TypeAvantageNature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvantageNatureDto {

    private Long id;
    private Long employeId;
    private TypeAvantageNature typeAvantage;
    private Boolean actif;

}
