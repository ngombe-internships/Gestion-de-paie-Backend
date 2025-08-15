package com.hades.maalipo.service.conge;

import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.UserRepository;
import com.hades.maalipo.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationCongeService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCongeService.class);

    private final EmailService emailService;
    private final DemandeCongeRepository demandeCongeRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationCongeService(
            EmailService emailService,
            DemandeCongeRepository demandeCongeRepository,
            UserRepository userRepository) {
        this.emailService = emailService;
        this.demandeCongeRepository = demandeCongeRepository;
        this.userRepository = userRepository;
    }

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    // Notifie le manager qu'une nouvelle demande de congé a été soumise
    public void notifierSoumissionDemande(DemandeConge demande) {
        if (!notificationsEnabled) return;

        // Récupérer l'employeur (manager) pour l'entreprise de l'employé
        User manager = getManagerEmploye(demande.getEmploye());
        if (manager == null) {
            logger.warn("Impossible d'envoyer la notification: manager introuvable");
            return;
        }

        Entreprise entreprise = demande.getEmploye().getEntreprise();
        String managerEmail = manager.getUsername(); // Fallback à l'username si pas d'email spécifique

        // Récupérer l'email du manager via l'entreprise si disponible
        if (entreprise != null && entreprise.getEmailEntreprise() != null && !entreprise.getEmailEntreprise().isEmpty()) {
            managerEmail = entreprise.getEmailEntreprise();
        }

        String sujet = "Nouvelle demande de congé à approuver";
        String message = String.format(
                "L'employé %s %s a soumis une demande de congé du %s au %s.\n\n" +
                        "Veuillez vous connecter à l'application pour approuver ou rejeter cette demande.",
                demande.getEmploye().getPrenom(),
                demande.getEmploye().getNom(),
                demande.getDateDebut(),
                demande.getDateFin()
        );

        String greeting = "Bonjour " + manager.getUsername() + ",";

        // Utiliser le nouveau service d'email avec template HTML
        emailService.sendCongeDetailedNotification(
                managerEmail,
                sujet,
                demande,
                greeting,
                message,
                entreprise
        );

        logger.info("Notification de soumission envoyée à l'employeur: {}", managerEmail);
    }

    // Notifie l'employé de la décision concernant sa demande de congé
    public void notifierDecisionEmploye(DemandeConge demande) {
        if (!notificationsEnabled) return;

        Employe employe = demande.getEmploye();
        String emailEmploye = employe.getEmail();

        if (emailEmploye == null || emailEmploye.isEmpty()) {
            logger.warn("Impossible d'envoyer la notification: email de l'employé non disponible");
            return;
        }

        Entreprise entreprise = employe.getEntreprise();
        String sujet;
        String message;
        String greeting = "Bonjour " + employe.getPrenom() + ",";

        if (demande.getStatut() == StatutDemandeConge.APPROUVEE) {
            sujet = "Votre demande de congé a été APPROUVÉE";
            message = "Votre demande de congé a été approuvée. Vous pouvez consulter les détails ci-dessous.";
        } else if (demande.getStatut() == StatutDemandeConge.REJETEE) {
            sujet = "Votre demande de congé a été REJETÉE";
            message = "Votre demande de congé a été rejetée." +
                    (demande.getMotifRejet() != null ? " Motif : " + demande.getMotifRejet() : "");
        } else {
            // Pas de notification pour les autres statuts
            return;
        }

        // Utiliser le nouveau service d'email avec template HTML
        emailService.sendCongeDetailedNotification(
                emailEmploye,
                sujet,
                demande,
                greeting,
                message,
                entreprise
        );

        logger.info("Notification de décision envoyée à l'employé: {}", emailEmploye);
    }

    // Envoie un rappel aux managers pour les demandes en attente depuis trop longtemps
    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    public void envoyerRappelsDemandesEnAttente() {
        if (!notificationsEnabled) return;

        logger.info("Exécution de la tâche planifiée: rappels des demandes en attente");

        // ✅ NOUVELLE VERSION: Calculer la date limite côté Java
        int joursAttente = 3;
        LocalDate dateLimit = LocalDate.now().minusDays(joursAttente);

        // Récupérer toutes les demandes en attente depuis plus de 3 jours
        List<DemandeConge> demandesEnAttente = demandeCongeRepository.findDemandesEnAttenteDepuisJours(
                StatutDemandeConge.EN_ATTENTE,
                dateLimit
        );

        logger.info("Nombre de demandes en attente depuis plus de {} jours: {}", joursAttente, demandesEnAttente.size());

        for (DemandeConge demande : demandesEnAttente) {
            try {
                // Récupérer le manager et son email
                User manager = getManagerEmploye(demande.getEmploye());
                if (manager == null) {
                    logger.warn("Aucun manager trouvé pour l'employé ID: {}", demande.getEmploye().getId());
                    continue;
                }

                Entreprise entreprise = demande.getEmploye().getEntreprise();
                String managerEmail = manager.getUsername(); // Fallback

                if (entreprise != null && entreprise.getEmailEntreprise() != null && !entreprise.getEmailEntreprise().isEmpty()) {
                    managerEmail = entreprise.getEmailEntreprise();
                }

                // Calculer les jours d'attente réels
                long joursAttenteReel = ChronoUnit.DAYS.between(demande.getDateDemande(), LocalDate.now());

                String sujet = "RAPPEL: Demande de congé en attente depuis " + joursAttenteReel + " jours";
                String greeting = "Bonjour " + manager.getUsername() + ",";
                String message = String.format(
                        "La demande de congé de %s %s est en attente de votre décision depuis %d jours.\n\n" +
                                "Détails de la demande:\n" +
                                "- Type: %s\n" +
                                "- Période: du %s au %s\n" +
                                "- Date de demande: %s\n\n" +
                                "Veuillez vous connecter à l'application pour traiter cette demande dans les plus brefs délais.",
                        demande.getEmploye().getPrenom(),
                        demande.getEmploye().getNom(),
                        joursAttenteReel,
                        demande.getTypeConge(),
                        demande.getDateDebut(),
                        demande.getDateFin(),
                        demande.getDateDemande()
                );

                // Utiliser le nouveau service d'email avec template HTML
                emailService.sendCongeDetailedNotification(
                        managerEmail,
                        sujet,
                        demande,
                        greeting,
                        message,
                        entreprise
                );

                logger.info("Rappel envoyé au manager (email: {}) pour la demande ID: {} (en attente depuis {} jours)",
                        managerEmail, demande.getId(), joursAttenteReel);

            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi du rappel pour la demande ID: {}", demande.getId(), e);
                // Continue avec la suivante sans interrompre le processus
            }
        }

        logger.info("Fin de l'exécution de la tâche planifiée: {} rappels traités", demandesEnAttente.size());
    }

    // Envoie un rappel aux employés la veille de leur retour de congé

    @Scheduled(cron = "0 0 16 * * *") // Tous les jours à 16h
    public void envoyerRappelsRetourCongé() {
        if (!notificationsEnabled) return;

        logger.info("Exécution de la tâche planifiée: rappels de retour de congé");

        // Récupérer tous les congés qui se terminent demain
        LocalDate demain = LocalDate.now().plusDays(1);
        List<DemandeConge> congesFinissantDemain = demandeCongeRepository.findByDateFinAndStatut(
                demain, StatutDemandeConge.APPROUVEE);

        for (DemandeConge demande : congesFinissantDemain) {
            Employe employe = demande.getEmploye();
            String emailEmploye = employe.getEmail();

            if (emailEmploye == null || emailEmploye.isEmpty()) {
                continue;
            }

            Entreprise entreprise = employe.getEntreprise();
            String sujet = "RAPPEL: Retour de congé demain";
            String greeting = "Bonjour " + employe.getPrenom() + ",";
            String message = String.format(
                    "Nous vous rappelons que votre congé se termine aujourd'hui. " +
                            "Vous êtes attendu(e) au travail demain, le %s.",
                    demain.plusDays(1)
            );

            // Utiliser le nouveau service d'email avec template HTML
            emailService.sendCongeDetailedNotification(
                    emailEmploye,
                    sujet,
                    demande,
                    greeting,
                    message,
                    entreprise
            );

            logger.info("Rappel de retour envoyé à l'employé: {}", emailEmploye);
        }
    }

    // Notifie l'annulation d'une demande de congé
    public void notifierAnnulationConge(DemandeConge demande) {
        if (!notificationsEnabled) return;

        Employe employe = demande.getEmploye();
        Entreprise entreprise = employe.getEntreprise();

        // Notifier l'employé
        String emailEmploye = employe.getEmail();
        if (emailEmploye != null && !emailEmploye.isEmpty()) {
            String sujet = "Demande de congé ANNULÉE";
            String greeting = "Bonjour " + employe.getPrenom() + ",";
            String message = String.format(
                    "Votre demande de congé du %s au %s a été ANNULÉE.\n\n" +
                            "Type de congé : %s\n" +
                            "Annulée par : %s\n" +
                            "Date d'annulation : %s",
                    demande.getDateDebut(),
                    demande.getDateFin(),
                    demande.getTypeConge().getLibelle(),
                    demande.getApprouveePar() != null ? getNomApprouveur(demande.getApprouveePar()) : "Administration",
                    demande.getDateApprobationRejet()
            );

            // Utiliser le nouveau service d'email avec template HTML
            emailService.sendCongeDetailedNotification(
                    emailEmploye,
                    sujet,
                    demande,
                    greeting,
                    message,
                    entreprise
            );

            logger.info("Notification d'annulation envoyée à l'employé: {}", emailEmploye);
        }

        // Si l'annulation est faite par l'employé, notifier l'employeur
        if (demande.getApprouveePar() != null && demande.getApprouveePar().getRole() == Role.EMPLOYE) {
            User manager = getManagerEmploye(employe);
            if (manager == null) return;

            String managerEmail = manager.getUsername(); // Fallback
            if (entreprise != null && entreprise.getEmailEntreprise() != null && !entreprise.getEmailEntreprise().isEmpty()) {
                managerEmail = entreprise.getEmailEntreprise();
            }

            String sujet = "Annulation de demande de congé";
            String greeting = "Bonjour " + manager.getUsername() + ",";
            String message = String.format(
                    "L'employé %s %s a annulé sa demande de congé du %s au %s.",
                    employe.getPrenom(),
                    employe.getNom(),
                    demande.getDateDebut(),
                    demande.getDateFin()
            );

            // Utiliser le nouveau service d'email avec template HTML
            emailService.sendCongeDetailedNotification(
                    managerEmail,
                    sujet,
                    demande,
                    greeting,
                    message,
                    entreprise
            );

            logger.info("Notification d'annulation envoyée à l'employeur: {}", managerEmail);
        }
    }


    //Récupère le manager d'un employé (l'employeur de son entreprise)
    private User getManagerEmploye(Employe employe) {
        if (employe == null || employe.getEntreprise() == null) {
            return null;
        }

        Long entrepriseId = employe.getEntreprise().getId();
        return userRepository.findFirstEmployeurByEntrepriseId(entrepriseId);
    }

    // Récupère le nom complet de l'approbateur/réjecteur
    private String getNomApprouveur(User approuveur) {
        if (approuveur.getRole() == Role.EMPLOYEUR) {
            if (approuveur.getEntreprise() != null) {
                return approuveur.getEntreprise().getNom() + " (Employeur)";
            }
            return approuveur.getUsername() + " (Employeur)";
        } else if (approuveur.getRole() == Role.ADMIN) {
            return "Administration";
        } else if (approuveur.getEmploye() != null) {
            return approuveur.getEmploye().getPrenom() + " " + approuveur.getEmploye().getNom();
        }

        return approuveur.getUsername();
    }
}