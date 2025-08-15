package com.hades.maalipo.dto.employe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeurListDto {
    private Long entrepriseId;
    private String usernameEmployeur;
    private String nomEntreprise;
    private LocalDate dateCreationEntreprise;
    private int nombreEmployes;
    private boolean active;
    private LocalDateTime dateCreationSysteme;
    private LocalDateTime dateDerniereMiseAJour;
}
