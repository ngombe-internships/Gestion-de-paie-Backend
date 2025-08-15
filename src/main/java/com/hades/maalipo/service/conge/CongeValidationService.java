package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.DemandeCongeCreateDto;
import com.hades.maalipo.dto.conge.TypeCongeConfigDTO;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.repository.DemandeCongeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CongeValidationService {

    private final SoldeCongeService soldeCongeService;
    private final DemandeCongeRepository demandeCongeRepository;
    private final CongesFamiliauxService congesFamiliauxService;
    private final CongeMaladieService congeMaladieService;
    private final TypeCongeConfigService typeCongeConfigService;
    private final JourFerieService jourFerieService;

    public CongeValidationService(
            SoldeCongeService soldeCongeService,
            DemandeCongeRepository demandeCongeRepository,
            CongesFamiliauxService congesFamiliauxService,
            CongeMaladieService congeMaladieService,
            TypeCongeConfigService typeCongeConfigService,
            JourFerieService jourFerieService
    ) {
        this.soldeCongeService = soldeCongeService;
        this.demandeCongeRepository = demandeCongeRepository;
        this.congesFamiliauxService = congesFamiliauxService;
        this.congeMaladieService = congeMaladieService;
        this.typeCongeConfigService = typeCongeConfigService;
        this.jourFerieService = jourFerieService;
    }

    //Validation complète d'une demande de congé AVANT soumission

    public ValidationResult validerDemandeConge(DemandeCongeCreateDto demande, Employe employe) {
        List<String> erreurs = new ArrayList<>();

        // 1. Validation période d'essai
        if (!validerPeriodeEssai(employe, demande.getTypeConge())) {
            erreurs.add("Période d'essai : Les congés payés ne sont autorisés qu'après 6 mois d'ancienneté");
        }

        // 2. Validation des dates
        if (!validerDates(demande)) {
            erreurs.add("Dates invalides : La date de fin doit être postérieure à la date de début");
        }

        // 3. Validation délai de préavis
        String erreurPreavis = validerDelaiPreavis(demande, employe);
        if (erreurPreavis != null) {
            erreurs.add(erreurPreavis);
        }

        // 4. Validation solde disponible (pour congés payés)
        if (demande.getTypeConge() == TypeConge.CONGE_PAYE) {
            long joursDemanges = calculerJoursOuvrables(demande.getDateDebut(), demande.getDateFin(), employe);
            if (!soldeCongeService.aSoldeSuffisant(employe, joursDemanges)) {
                erreurs.add("Solde insuffisant : Vous n'avez pas assez de jours de congés payés disponibles");
            }
        }

        // 5. Validation chevauchement
        if (detecterChevauchement(employe, demande)) {
            erreurs.add("Chevauchement détecté : Une demande existe déjà sur cette période");
        }

        // 6. Validations spécifiques par type
        erreurs.addAll(validerSelonType(demande, employe));

        // 7. Validation durée maximale
        String erreurDuree = validerDureeMaximale(demande, employe);
        if (erreurDuree != null) {
            erreurs.add(erreurDuree);
        }

        return new ValidationResult(erreurs.isEmpty(), erreurs);
    }

    //Validation période d'essai

    private boolean validerPeriodeEssai(Employe employe, TypeConge typeConge) {
        if (typeConge != TypeConge.CONGE_PAYE) {
            return true; // Seuls les congés payés sont soumis à la période d'essai
        }
        return !soldeCongeService.estEnPeriodeEssai(employe);
    }

    //Validation des dates
    private boolean validerDates(DemandeCongeCreateDto demande) {
        return demande.getDateDebut() != null &&
                demande.getDateFin() != null &&
                !demande.getDateFin().isBefore(demande.getDateDebut()) &&
                !demande.getDateDebut().isBefore(LocalDate.now());
    }

    //Validation délai de préavis
    private String validerDelaiPreavis(DemandeCongeCreateDto demande, Employe employe) {
        // Utilisation d'une variable AtomicInteger pour contourner le problème "effectively final"
        AtomicInteger delaiRequis = new AtomicInteger(demande.getTypeConge().getDelaiPreavisJours());

        // Vérifier configuration d'entreprise
        Optional<TypeCongeConfigDTO> configOpt = typeCongeConfigService.getConfigByEntrepriseAndType(
                employe.getEntreprise().getId(),
                demande.getTypeConge()
        );

        if (configOpt.isPresent()) {
            TypeCongeConfigDTO config = configOpt.get();
            if (config.getDelaiPreavisJours() != null) {
                delaiRequis.set(config.getDelaiPreavisJours());
            }
        }

        long joursAvance = ChronoUnit.DAYS.between(LocalDate.now(), demande.getDateDebut());

        if (joursAvance < delaiRequis.get()) {
            return String.format("Délai de préavis insuffisant : %d jours requis, %d jours fournis",
                    delaiRequis.get(), joursAvance);
        }

        return null;
    }

    //Détection de chevauchement
    private boolean detecterChevauchement(Employe employe, DemandeCongeCreateDto demande) {
        List<StatutDemandeConge> statutsActifs = Arrays.asList(
                StatutDemandeConge.EN_ATTENTE,
                StatutDemandeConge.APPROUVEE
        );

        List<DemandeConge> demandesExistantes =
                demandeCongeRepository.findOverlappingDemandesForEmploye(
                        employe, statutsActifs, demande.getDateDebut(), demande.getDateFin());

        return !demandesExistantes.isEmpty();
    }

    //Validations spécifiques par type de congé
    private List<String> validerSelonType(DemandeCongeCreateDto demande, Employe employe) {
        List<String> erreurs = new ArrayList<>();

        // Récupérer la configuration pour ce type de congé (une seule fois pour toute la méthode)
        Optional<TypeCongeConfigDTO> configOpt = typeCongeConfigService.getConfigByEntrepriseAndType(
                employe.getEntreprise().getId(),
                demande.getTypeConge()
        );

        // Variable pour stocker si les documents sont obligatoires
        Boolean docOblig = configOpt.isPresent() ? configOpt.get().getDocumentsObligatoires() : null;
        boolean documentsObligatoires = Boolean.TRUE.equals(docOblig);

        switch (demande.getTypeConge()) {
            case CONGE_MATERNITE:
                long semaines = ChronoUnit.WEEKS.between(demande.getDateDebut(), demande.getDateFin().plusDays(1));
                if (!congesFamiliauxService.estCongeMaterniteValide(employe, (int) semaines)) {
                    erreurs.add("Congé maternité : Durée maximale de 14 semaines dépassée");
                }

                // Vérifier les documents uniquement s'ils sont obligatoires
                if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                    erreurs.add("Congé maternité : Certificat de grossesse requis");
                }
                break;

            case CONGE_PATERNITE:
                long joursPaternite = ChronoUnit.DAYS.between(demande.getDateDebut(), demande.getDateFin().plusDays(1));
                if (!congesFamiliauxService.estCongePaterniteValide(employe, (int) joursPaternite)) {
                    erreurs.add("Congé paternité : Durée autorisée de 2 jours ouvrables uniquement");
                }

                // Vérifier les documents uniquement s'ils sont obligatoires
                if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                    erreurs.add("Congé paternité : Acte de naissance requis");
                }
                break;

            case CONGE_MARIAGE:
                if (!congesFamiliauxService.estCongeMariageValide(employe)) {
                    erreurs.add("Congé mariage : Vous avez déjà bénéficié de ce congé");
                }

                // Vérifier les documents uniquement s'ils sont obligatoires
                if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                    erreurs.add("Congé mariage : Pièces justificatives requises (acte de mariage)");
                }
                break;

            case CONGE_DEUIL:
                // Le lien familial est toujours obligatoire, indépendamment des documents
                if (demande.getLienFamilial() == null) {
                    erreurs.add("Congé deuil : Le lien familial avec le défunt est obligatoire");
                } else {
                    long joursDeuil = ChronoUnit.DAYS.between(demande.getDateDebut(), demande.getDateFin().plusDays(1));
                    if (!congesFamiliauxService.estCongeDeuilValide(employe, (int) joursDeuil, demande.getLienFamilial())) {
                        erreurs.add("Congé deuil : Lien familial non éligible ou durée incorrecte");
                    }

                    // Vérifier les documents uniquement s'ils sont obligatoires
                    if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                        erreurs.add("Congé deuil : Acte de décès et preuve du lien familial requis");
                    }
                }
                break;

            case CONGE_MALADIE:
            case CONGE_ACCIDENT_TRAVAIL:
                // Pour les congés médicaux, la validation des documents est toujours importante,
                // mais on respecte quand même la configuration de l'entreprise
                if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                    erreurs.add("Congé maladie : Certificat médical obligatoire");
                }

                // Vérification des droits selon l'ancienneté
                String droitsMaladie = congeMaladieService.determinerDroitsCongeMaladie(employe);
                if (droitsMaladie.startsWith("Période d'essai")) {
                    erreurs.add("Congé maladie : " + droitsMaladie);
                }

                // Vérification de la durée selon l'ancienneté
                long joursMaladie = ChronoUnit.DAYS.between(demande.getDateDebut(), demande.getDateFin().plusDays(1));
                if (!congeMaladieService.validerDroitsEtDuree(employe, (int) joursMaladie)) {
                    erreurs.add("Congé maladie : Durée demandée dépasse vos droits selon votre ancienneté");
                }
                break;

            // Ajouter les autres types de congé si nécessaire
            default:
                // Validations par défaut pour autres types de congé
                if (documentsObligatoires && (demande.getDocumentsJustificatifs() == null || demande.getDocumentsJustificatifs().isEmpty())) {
                    erreurs.add("Documents justificatifs requis pour ce type de congé");
                }
                break;
        }

        return erreurs;
    }


    // Validation durée maximale
    private String validerDureeMaximale(DemandeCongeCreateDto demande, Employe employe) {
        AtomicInteger dureeMaximale = new AtomicInteger(demande.getTypeConge().getDureeMaximaleJours());

        // Vérifier configuration d'entreprise
        Optional<TypeCongeConfigDTO> configOpt = typeCongeConfigService.getConfigByEntrepriseAndType(
                employe.getEntreprise().getId(),
                demande.getTypeConge()
        );

        if (configOpt.isPresent()) {
            TypeCongeConfigDTO config = configOpt.get();
            if (config.getDureeMaximaleJours() != null) {
                dureeMaximale.set(config.getDureeMaximaleJours());
            }
        }

        long joursDemanges = calculerJoursOuvrables(demande.getDateDebut(), demande.getDateFin(), employe);

        if (joursDemanges > dureeMaximale.get()) {
            return String.format("Durée maximale dépassée : %d jours maximum autorisés", dureeMaximale.get());
        }

        return null;
    }

    // Calcul des jours ouvrables
    private long calculerJoursOuvrables(LocalDate dateDebut, LocalDate dateFin, Employe employe) {
        long joursOuvrables = 0;

        for (LocalDate date = dateDebut; !date.isAfter(dateFin); date = date.plusDays(1)) {
            // Exclure weekends
            if (date.getDayOfWeek().getValue() < 6) { // Lundi=1 à Vendredi=5
                // Exclure jours fériés
                if (!jourFerieService.isJourFerie(date, employe.getEntreprise().getId())) {
                    joursOuvrables++;
                }
            }
        }

        return joursOuvrables;
    }

    //Classe pour le résultat de validation
    public static class ValidationResult {
        private final boolean valide;
        private final List<String> erreurs;

        public ValidationResult(boolean valide, List<String> erreurs) {
            this.valide = valide;
            this.erreurs = erreurs;
        }

        public boolean isValide() { return valide; }
        public List<String> getErreurs() { return erreurs; }
        public String getMessageErreurs() { return String.join("; ", erreurs); }
    }
}