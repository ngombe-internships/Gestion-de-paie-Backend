package com.hades.maalipo.dto;

import com.hades.maalipo.enum1.TypeAvantageNature;
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
