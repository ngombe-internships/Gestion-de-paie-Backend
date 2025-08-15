package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.AbsenceJourneeDto;
import com.hades.maalipo.dto.conge.CalendrierMoisDto;
import com.hades.maalipo.dto.conge.EffectifJournalierDto;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.utils.JourOuvrableCalculatorUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendrierCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final JourFerieService jourFerieService;
    private final JourOuvrableCalculatorUtil jourOuvrableCalculator;

    public CalendrierCongeService ( DemandeCongeRepository demandeCongeRepository,
                                    EmployeRepository employeRepository,
                                    JourFerieService jourFerieService,
                                    JourOuvrableCalculatorUtil jourOuvrableCalculator){
        this.jourFerieService =jourFerieService;
        this.employeRepository = employeRepository;
        this.demandeCongeRepository = demandeCongeRepository;
        this.jourOuvrableCalculator = jourOuvrableCalculator;
    }

    //Récupère les absences mensuelles pour une entreprise donnée
    // Inclut la détection de sous-effectif et les conflits potentiels

    public List<AbsenceJourneeDto> getAbsencesMensuelles(Entreprise entreprise, int annee, int mois) {
        // Définit la période du mois à analyser
        LocalDate debutMois = LocalDate.of(annee, mois, 1);
        LocalDate finMois = debutMois.plusMonths(1).minusDays(1);

        // Récupère toutes les demandes de congé approuvées ou en attente pour la période
        List<DemandeConge> demandes = demandeCongeRepository.findByEntrepriseAndDateRange(
                entreprise.getId(), debutMois, finMois, StatutDemandeConge.APPROUVEE, StatutDemandeConge.EN_ATTENTE
        );

        // Structures pour analyser les absences par jour
        Map<LocalDate, Integer> absentsParJour = new HashMap<>();
        Map<LocalDate, List<AbsenceJourneeDto>> absencesParJour = new HashMap<>();

        // Pour chaque demande de congé, décomposer en absences journalières
        for (DemandeConge demande : demandes) {
            // Parcourt tous les jours de la période de congé
            LocalDate dateCourante = demande.getDateDebut();
            while (!dateCourante.isAfter(demande.getDateFin())) {
                // Ne compte que les jours ouvrables (ni weekend, ni jour férié)
                if (estJourOuvrable(dateCourante, entreprise.getId())) {
                    // Incrémente le compteur d'absents pour ce jour
                    absentsParJour.put(dateCourante, absentsParJour.getOrDefault(dateCourante, 0) + 1);

                    // Crée une entrée d'absence pour ce jour
                    AbsenceJourneeDto absenceDto = mapToAbsenceJourneeDto(demande, dateCourante);

                    // Ajoute à la liste des absences pour ce jour
                    if (!absencesParJour.containsKey(dateCourante)) {
                        absencesParJour.put(dateCourante, new ArrayList<>());
                    }
                    absencesParJour.get(dateCourante).add(absenceDto);
                }
                // Passe au jour suivant
                dateCourante = dateCourante.plusDays(1);
            }
        }

        // Calcule l'effectif total de l'entreprise (pour détecter le sous-effectif)
        int effectifTotal = getEffectifTotal(entreprise);

        // Convertit la map en liste plate d'absences avec marqueurs de conflit
        List<AbsenceJourneeDto> resultat = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AbsenceJourneeDto>> entry : absencesParJour.entrySet()) {
            LocalDate date = entry.getKey();
            int absentsAujourdhui = absentsParJour.get(date);

            // Définit le seuil de sous-effectif à 30% d'absents
            boolean sousEffectif = absentsAujourdhui > (effectifTotal * 0.3);

            // Marque chaque absence du jour avec l'indicateur de sous-effectif
            for (AbsenceJourneeDto absence : entry.getValue()) {
                absence.setSousEffectif(sousEffectif);

                // Calcule le taux d'absentéisme pour information
                double tauxAbsenteisme = (double) absentsAujourdhui / effectifTotal * 100;
                absence.setTauxAbsenteisme(Math.round(tauxAbsenteisme * 10) / 10.0); // Arrondi à 1 décimale

                resultat.add(absence);
            }
        }

        return resultat;
    }


    // Génère un calendrier mensuel complet avec les jours ouvrables, fériés et le taux d'occupation
    public CalendrierMoisDto getCalendrierMensuel(Entreprise entreprise, int annee, int mois) {
        // Crée le DTO du calendrier mensuel
        CalendrierMoisDto calendrier = new CalendrierMoisDto(annee, mois);

        // Définit la période du mois
        LocalDate debutMois = LocalDate.of(annee, mois, 1);
        LocalDate finMois = debutMois.plusMonths(1).minusDays(1);

        // Récupère toutes les demandes de congé pour ce mois
        List<DemandeConge> demandes = demandeCongeRepository.findByEntrepriseAndDateRange(
                entreprise.getId(), debutMois, finMois, StatutDemandeConge.APPROUVEE, StatutDemandeConge.EN_ATTENTE
        );

        // Effectif total de l'entreprise
        int effectifTotal = getEffectifTotal(entreprise);
        calendrier.setEffectifTotal(effectifTotal);

        // Pour chaque jour du mois
        for (int jour = 1; jour <= finMois.getDayOfMonth(); jour++) {
            LocalDate date = LocalDate.of(annee, mois, jour);

            // Calcule l'effectif pour ce jour
            EffectifJournalierDto effectif = calculerEffectifJournalier(date, demandes, entreprise, effectifTotal);

            // Ajoute au calendrier
            calendrier.getJours().put(jour, effectif);
        }

        return calendrier;
    }

    // Calcule les effectifs présents/absents pour un jour donné
    private EffectifJournalierDto calculerEffectifJournalier(LocalDate date, List<DemandeConge> demandes,
                                                             Entreprise entreprise, int effectifTotal) {
        // Crée le DTO d'effectif journalier
        EffectifJournalierDto effectif = new EffectifJournalierDto();
        effectif.setDate(date);
        effectif.setJourOuvrable(estJourOuvrable(date, entreprise.getId()));
        effectif.setJourFerie(jourFerieService.isJourFerie(date, entreprise.getId()));

        // Si ce n'est pas un jour ouvrable, pas besoin de calculer les effectifs
        if (!effectif.isJourOuvrable()) {
            effectif.setPresents(0);
            effectif.setAbsents(0);
            effectif.setTauxOccupation(0);
            return effectif;
        }

        // Compte les absences pour ce jour
        int absents = 0;
        for (DemandeConge demande : demandes) {
            if ((date.isEqual(demande.getDateDebut()) || date.isAfter(demande.getDateDebut())) &&
                    (date.isEqual(demande.getDateFin()) || date.isBefore(demande.getDateFin()))) {
                absents++;
            }
        }

        // Calcule les présents et le taux d'occupation
        int presents = effectifTotal - absents;
        double tauxOccupation = (double) presents / effectifTotal * 100;

        effectif.setPresents(presents);
        effectif.setAbsents(absents);
        effectif.setTauxOccupation(Math.round(tauxOccupation * 10) / 10.0); // Arrondi à 1 décimale

        // Détecte le sous-effectif
        effectif.setSousEffectif(absents > (effectifTotal * 0.3));

        return effectif;
    }

    // Récupère les demandes en attente pour un gestionnaire
    public List<AbsenceJourneeDto> getDemandesEnAttente(Entreprise entreprise) {
        // Récupère toutes les demandes en attente
        List<DemandeConge> demandesEnAttente = demandeCongeRepository.findByEntrepriseAndStatut(
                entreprise.getId(), StatutDemandeConge.EN_ATTENTE
        );

        // Trie par date de demande (plus anciennes d'abord)
        demandesEnAttente.sort(Comparator.comparing(DemandeConge::getDateDemande));

        // Convertit en DTOs
        return demandesEnAttente.stream()
                .map(demande -> mapToAbsenceJourneeDto(demande, null))
                .collect(Collectors.toList());
    }


    // Détecte les conflits potentiels (sous-effectif) pour la période à venir
    public List<EffectifJournalierDto> detecterConflitsPotentiels(Entreprise entreprise, int nbJours) {
        LocalDate dateDebut = LocalDate.now();
        LocalDate dateFin = dateDebut.plusDays(nbJours);

        // Récupère toutes les demandes approuvées et en attente pour la période
        List<DemandeConge> demandes = demandeCongeRepository.findByEntrepriseAndDateRange(
                entreprise.getId(), dateDebut, dateFin,
                StatutDemandeConge.APPROUVEE, StatutDemandeConge.EN_ATTENTE
        );

        int effectifTotal = getEffectifTotal(entreprise);
        // Analyse jour par jour
        List<EffectifJournalierDto> joursConflictuels = new ArrayList<>();

        for (LocalDate date = dateDebut; !date.isAfter(dateFin); date = date.plusDays(1)) {
            // Ne traite que les jours ouvrables
            if (estJourOuvrable(date, entreprise.getId())) {
                EffectifJournalierDto effectif = calculerEffectifJournalier(date, demandes, entreprise, effectifTotal);

                // Ajoute à la liste des jours conflictuels si sous-effectif
                if (effectif.isSousEffectif()) {
                    joursConflictuels.add(effectif);
                }
            }
        }
        return joursConflictuels;
    }

    //Analyse l'impact d'une nouvelle demande de congé sur les effectifs
    public List<EffectifJournalierDto> analyserImpactDemandeConge(
            Entreprise entreprise, LocalDate dateDebut, LocalDate dateFin) {

        // Récupère les demandes existantes pour cette période
        List<DemandeConge> demandesExistantes = demandeCongeRepository.findByEntrepriseAndDateRange(
                entreprise.getId(), dateDebut, dateFin,
                StatutDemandeConge.APPROUVEE, StatutDemandeConge.EN_ATTENTE
        );

        int effectifTotal = getEffectifTotal(entreprise);

        // Simule l'ajout d'une personne absente supplémentaire
        List<EffectifJournalierDto> joursImpactes = new ArrayList<>();

        for (LocalDate date = dateDebut; !date.isAfter(dateFin); date = date.plusDays(1)) {
            if (estJourOuvrable(date, entreprise.getId())) {
                EffectifJournalierDto effectifActuel = calculerEffectifJournalier(
                        date, demandesExistantes, entreprise, effectifTotal);

                // Simule un absent de plus
                int nouveauxAbsents = effectifActuel.getAbsents() + 1;
                boolean nouveauSousEffectif = nouveauxAbsents > (effectifTotal * 0.3);

                // Si ça crée un sous-effectif alors qu'il n'y en avait pas
                if (nouveauSousEffectif && !effectifActuel.isSousEffectif()) {
                    EffectifJournalierDto impact = new EffectifJournalierDto();
                    impact.setDate(date);
                    impact.setPresents(effectifActuel.getPresents() - 1);
                    impact.setAbsents(nouveauxAbsents);
                    impact.setSousEffectif(true);
                    impact.setTauxOccupation(
                            Math.round((double)(effectifActuel.getPresents() - 1) / effectifTotal * 1000) / 10.0);

                    joursImpactes.add(impact);
                }
            }
        }

        return joursImpactes;
    }


    // Vérifie si une date est un jour ouvrable

    private boolean estJourOuvrable(LocalDate date, Long entrepriseId) {
        return jourOuvrableCalculator.estJourOuvrable(date, entrepriseId);
    }
    // Convertit une demande de congé en DTO d'absence journalière
    private AbsenceJourneeDto mapToAbsenceJourneeDto(DemandeConge demande, LocalDate date) {
        AbsenceJourneeDto dto = new AbsenceJourneeDto(demande);

        // Si une date spécifique est fournie, la définir dans le DTO
        if (date != null) {
            dto.setDateSpecifique(date);
        }

        return dto;
    }

    // Récupère l'effectif total d'une entreprise
    private int getEffectifTotal(Entreprise entreprise) {
        int count = employeRepository.countByEntrepriseIdAndActif(entreprise.getId(), true);
        // S'assurer que l'effectif n'est jamais nul pour éviter les divisions par zéro
        return Math.max(1, count);
    }

    // Génère les données d'exportation pour calendriers externes (iCal, Google)
    public String genererDonneesExportCalendrier(Entreprise entreprise, int annee) {
        // Cette méthode générerait les données au format iCal
        // Exemple simplifié - une implémentation complète utiliserait une librairie iCal
        StringBuilder icalData = new StringBuilder();
        icalData.append("BEGIN:VCALENDAR\n");
        icalData.append("VERSION:2.0\n");
        icalData.append("PRODID:-//Maalipo//CalendrierConges//FR\n");

        // Récupère toutes les absences de l'année
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);

        List<DemandeConge> absencesAnnuelles = demandeCongeRepository.findByEntrepriseAndDateRange(
                entreprise.getId(), debut, fin, StatutDemandeConge.APPROUVEE, null);

        // Génère un événement iCal pour chaque absence
        for (DemandeConge absence : absencesAnnuelles) {
            icalData.append("BEGIN:VEVENT\n");
            icalData.append("SUMMARY:").append(absence.getEmploye().getNom()).append(" - ")
                    .append(absence.getTypeConge()).append("\n");
            icalData.append("DTSTART:").append(formatICalDate(absence.getDateDebut())).append("\n");
            icalData.append("DTEND:").append(formatICalDate(absence.getDateFin().plusDays(1))).append("\n");
            icalData.append("DESCRIPTION:").append(absence.getRaison() != null ? absence.getRaison() : "").append("\n");
            icalData.append("END:VEVENT\n");
        }

        icalData.append("END:VCALENDAR");
        return icalData.toString();
    }



    //Formate une date pour iCal
    private String formatICalDate(LocalDate date) {
        // Format requis par iCal: YYYYMMDD
        return date.toString().replaceAll("-", "");
    }
}