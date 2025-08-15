package com.hades.maalipo.service.conge;

import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.repository.DemandeCongeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Service
public class CongesFamiliauxService {

    private final DemandeCongeRepository demandeCongeRepository;

    public CongesFamiliauxService(DemandeCongeRepository demandeCongeRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
    }

    public boolean estCongeMaterniteValide(Employe employe, int semainesDemandes) {
        // La législation prévoit 14 semaines, fractionnement possible (4 avant, 10 après)
        return semainesDemandes <= 14;
    }

    public boolean estCongePaterniteValide(Employe employe, int joursDemandes) {
        // La législation prévoit 2 jours ouvrables à la naissance
        return joursDemandes == 2;
    }

    public boolean estCongeMariageValide(Employe employe) {
        // Le congé de mariage est autorisé une seule fois dans la carrière de l'employé
        Optional<DemandeConge> congeMarriageAnterior = demandeCongeRepository.findMariageLeave(employe.getId());
        return congeMarriageAnterior.isEmpty();
    }

    public boolean estCongeDeuilValide(Employe employe, int joursDemandes, String lienFamilial) {
        // La législation prévoit 2 jours selon un lien familial précis (conjoint, enfant, père, mère)
        if (lienFamilial == null) {
            return false;
        }

        int dureeMaximale = getDureeMaximaleCongeSelon(lienFamilial);
        return dureeMaximale > 0 && joursDemandes <= dureeMaximale;
    }

    //Calcule la durée maximale autorisée pour un congé familial spécifique
    public int getDureeMaximaleCongeSelon(String lienFamilial) {
        if (lienFamilial == null) {
            return 0;
        }

        switch (lienFamilial.toLowerCase()) {
            case "conjoint":
            case "enfant":
                return 3; // Certaines conventions peuvent aller jusqu'à 5 jours
            case "parent":  // père ou mère
                return 3;
            case "frere":
            case "soeur":
                return 2;
            case "beau_parent": // beaux-parents
            case "oncle":
            case "tante":
            case "cousin":
            case "cousine":
            case "neveu":
            case "niece":
                return 1;
            default:
                return 0; // Autres relations non couvertes par la législation
        }
    }
    //Vérifie si l'employé a droit à un congé pour évènement familial

    public boolean aDroitCongeEvenementFamilial(Employe employe) {
        // Vérifier que l'employé a au moins 3 mois d'ancienneté
        LocalDate dateEmbauche = employe.getDateEmbauche();
        LocalDate aujourdhui = LocalDate.now();
        Period period = Period.between(dateEmbauche, aujourdhui);

        // Calculer le nombre total de mois
        int mois = period.getYears() * 12 + period.getMonths();
        return mois >= 3;
    }
}