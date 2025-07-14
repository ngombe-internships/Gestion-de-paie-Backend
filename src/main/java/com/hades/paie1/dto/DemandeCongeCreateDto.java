package com.hades.paie1.dto;

import com.hades.paie1.enum1.TypeConge;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DemandeCongeCreateDto {
    @NotNull(message = "L'ID de l'employé est obligatoire")
    private Long employeId;

    @NotNull(message = "La date de début est obligatoire")
    @FutureOrPresent(message = "La date de début doit être dans le futur ou aujourd'hui")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @NotNull(message = "Le type de congé est obligatoire")
    private TypeConge typeConge;

    private String raison;
}
