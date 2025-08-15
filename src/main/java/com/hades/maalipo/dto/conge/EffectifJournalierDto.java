package com.hades.maalipo.dto.conge;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class EffectifJournalierDto {
    private LocalDate date;
    private int presents;
    private int absents;
    private double tauxOccupation;
    private boolean jourOuvrable;
    private boolean jourFerie;
    private boolean sousEffectif;
}