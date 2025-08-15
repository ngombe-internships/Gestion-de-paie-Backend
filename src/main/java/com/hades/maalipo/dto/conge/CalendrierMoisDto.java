package com.hades.maalipo.dto.conge;

import lombok.Getter;
import lombok.Setter;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class CalendrierMoisDto {
    private int annee;
    private int mois;
    private String nomMois;
    private int effectifTotal;
    private Map<Integer, EffectifJournalierDto> jours = new HashMap<>();

    public CalendrierMoisDto(int annee, int mois) {
        this.annee = annee;
        this.mois = mois;
        this.nomMois = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }
}