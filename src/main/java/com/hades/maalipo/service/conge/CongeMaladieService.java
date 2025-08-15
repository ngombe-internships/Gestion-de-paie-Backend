package com.hades.maalipo.service.conge;

import com.hades.maalipo.model.Employe;
import com.hades.maalipo.service.calculators.AncienneteService;
import org.springframework.stereotype.Service;

@Service
public class CongeMaladieService {

    private final AncienneteService ancienneteService;

    public CongeMaladieService  (AncienneteService ancienneteService){
        this.ancienneteService = ancienneteService;
    }

    public String determinerDroitsCongeMaladie(Employe employe) {
        int ancienneteEnMois = ancienneteService.calculAncienneteEnMois(employe.getDateEmbauche());

        if (ancienneteEnMois < 6) {
            return "Période d'essai : Congé maladie non rémunéré.";
        } else if (ancienneteEnMois <= 24) { // 6 mois à 2 ans
            return "Période de 6 mois à 2 ans : Période courte payée + mi-salaire.";
        } else { // Plus de 2 ans
            return "Plus de 2 ans : Période longue payée + mi-salaire.";
        }
    }

    public boolean validerDroitsEtDuree(Employe employe, int joursDemandes) {
        int ancienneteEnMois = ancienneteService.calculAncienneteEnMois(employe.getDateEmbauche());

        // En période d'essai
        if (ancienneteEnMois < 6) {
            return false; // Pas de congé maladie rémunéré en période d'essai
        }
        // Entre 6 mois et 2 ans
        if (ancienneteEnMois <= 24) {
            return joursDemandes <= 30; // Maximum 30 jours
        }
        // Plus de 2 ans
        return joursDemandes <= 90; // Maximum 90 jours
    }
}
