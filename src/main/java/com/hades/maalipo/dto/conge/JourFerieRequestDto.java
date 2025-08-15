package com.hades.maalipo.dto.conge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class JourFerieRequestDto {
    @NotBlank(message = "Le nom du jour férié est obligatoire")
    private String nom;

    @NotNull(message = "La date du jour férié est obligatoire")
    private LocalDate dateFerie;

    @NotNull(message = "Veuillez préciser si le jour est chômé et payé")
    private Boolean estChomeEtPaye;

    @NotNull(message = "L'ID de l'entreprise est obligatoire")
    private Long entrepriseId;
}