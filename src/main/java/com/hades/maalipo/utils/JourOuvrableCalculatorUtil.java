package com.hades.maalipo.utils;

import com.hades.maalipo.model.Employe;
import com.hades.maalipo.service.conge.JourFerieService;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class JourOuvrableCalculatorUtil {

    private final JourFerieService jourFerieService;

    public JourOuvrableCalculatorUtil(JourFerieService jourFerieService) {
        this.jourFerieService = jourFerieService;
    }

    /**
     * Calcule les jours ouvrables entre deux dates selon le contrat de l'employé
     */
    public long calculateWorkingDays(LocalDate dateDebut, LocalDate dateFin, Employe employe) {
        long workingDays = 0;
        LocalDate current = dateDebut;

        // Récupération des jours ouvrables contractuels
        Integer joursOuvrablesContractuels = employe.getJoursOuvrablesContractuelsHebdomadaires();
        if (joursOuvrablesContractuels == null) {
            joursOuvrablesContractuels = 5; // 5 jours par défaut (L-V)
        }

        while (!current.isAfter(dateFin)) {
            // Vérifier si c'est un jour férié
            boolean isJourFerie = jourFerieService.isJourFerie(current, employe.getEntreprise().getId());

            // Déterminer si c'est un jour ouvrable selon le contrat
            boolean isJourOuvrableContrat = estJourOuvrableSelonContrat(current, joursOuvrablesContractuels);

            // Compter seulement si ce n'est PAS un jour férié ET si c'est ouvrable selon le contrat
            if (!isJourFerie && isJourOuvrableContrat) {
                workingDays++;
            }

            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * Calcule les jours ouvrables avec paramètres d'entreprise
     */
    public long calculateWorkingDays(LocalDate dateDebut, LocalDate dateFin,
                                     Long entrepriseId, int joursOuvrablesHebdomadaires) {
        long workingDays = 0;
        LocalDate current = dateDebut;

        while (!current.isAfter(dateFin)) {
            boolean isJourFerie = jourFerieService.isJourFerie(current, entrepriseId);
            boolean isWorkingDay = estJourOuvrableSelonContrat(current, joursOuvrablesHebdomadaires);

            if (!isJourFerie && isWorkingDay) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Détermine si un jour est ouvrable selon le contrat
     */
    private boolean estJourOuvrableSelonContrat(LocalDate date, int joursOuvrablesHebdomadaires) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        switch (joursOuvrablesHebdomadaires) {
            case 5:  // Lundi à Vendredi
                return dayOfWeek.getValue() <= 5; // 1=Lundi, 5=Vendredi

            case 6:  // Lundi à Samedi
                return dayOfWeek.getValue() <= 6; // 1=Lundi, 6=Samedi

            case 4:  // Exemple : Lundi à Jeudi
                return dayOfWeek.getValue() <= 4; // 1=Lundi, 4=Jeudi

            case 7:  // Tous les jours (rare mais possible)
                return true;

            default: // Par défaut : 5 jours (L-V)
                return dayOfWeek.getValue() <= 5;
        }
    }

    /**
     * Vérifie si une date est un jour ouvrable (pour le calendrier)
     */
    public boolean estJourOuvrable(LocalDate date, Long entrepriseId) {
        DayOfWeek jour = date.getDayOfWeek();
        return jour != DayOfWeek.SATURDAY &&
                jour != DayOfWeek.SUNDAY &&
                !jourFerieService.isJourFerie(date, entrepriseId);
    }
}