package com.hades.paie1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeurListDto {
    private Long entrepriseId;
    private String usernameEmployeur;
    private String nomEntreprise;
    private LocalDate dateCreationEntreprise;
}
