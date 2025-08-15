package com.hades.maalipo.dto.conge;

import com.hades.maalipo.enum1.TypeConge;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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

    private List<String> documentsJustificatifs;
    private String lienFamilial; // Pour congé deuil
    private String certificatMedical;
}
