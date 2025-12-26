package com.hades.maalipo.service.conge;

import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.model.*;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.UserRepository;
import com.hades.maalipo.service.NotificationService;
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
    private final NotificationService notificationService;

    @Autowired
    public NotificationCongeService(
            EmailService emailService,
            DemandeCongeRepository demandeCongeRepository,
            UserRepository userRepository,
            NotificationService notificationService ) {
        this.emailService = emailService;
        this.demandeCongeRepository = demandeCongeRepository;
        this.userRepository = userRepository;
        this.notificationService  = notificationService;
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

        // ✅ CORRECTION : Try-Catch pour empêcher le rollback en cas d'erreur email
        try {
            // Utiliser le nouveau service d'email avec template HTML
            emailService.sendCongeDetailedNotification(
                    managerEmail,
                    sujet,
                    demande,
                    greeting,
                    message,
                    entreprise
            );
            logger.info("Email de soumission envoyé à l'employeur: {}", managerEmail);
        } catch (Exception e) {
            logger.error("⚠️ ÉCHEC envoi email soumission (mais la demande est sauvegardée) : {}", e.getMessage());
            // On continue sans lancer d'exception
        }

        // Notification interne (Base de données / WebSocket)
        try {
            notificationService.creerNotification(
                    manager.getId(),
                    "Nouvelle demande de congé",
                    String.format("%s %s a soumis une demande de congé du %s au %s",
                            demande.getEmploye().getPrenom(),
                            demande.getEmploye().getNom(),
                            demande.getDateDebut(),
                            demande.getDateFin()),
                    Notification.TypeNotification.DEMANDE_CONGE_SOUMISE,
                    demande.getId(),
                    "DEMANDE_CONGE"
            );
        } catch (Exception e) {
            logger.error("Erreur création notification interne: {}", e.getMessage());
        }
    }

    // Notifie l'employé de la décision concernant sa demande de congé
    public void notifierDecisionEmploye(DemandeConge demande) {
        if (!notificationsEnabled) return;

        Employe employe = demande.getEmploye();
        String emailEmploye = employe.getEmail();

        if (emailEmploye == null || emailEmploye.isEmpty()) {
            logger.warn("Impossible d'envoyer la notification: email de l'employé non disponible");
            // On continue quand même pour la notification interne
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
            return;
        }

        // ✅ CORRECTION : Try-Catch pour l'email
        if (emailEmploye != null && !emailEmploye.isEmpty()) {
            try {
                emailService.sendCongeDetailedNotification(
                        emailEmploye,
                        sujet,
                        demande,
                        greeting,
                        message,
                        entreprise
                );
                logger.info("Email de décision envoyé à l'employé: {}", emailEmploye);
            } catch (Exception e) {
                logger.error("⚠️ ÉCHEC envoi email décision : {}", e.getMessage());
            }
        }

        // Notification en base
        User employeUser = employe.getUser();
        if (employeUser == null && emailEmploye != null) {
            employeUser = userRepository.findByUsername(emailEmploye).orElse(null);
        }

        if (employeUser != null) {
            Notification.TypeNotification type = demande.getStatut() == StatutDemandeConge.APPROUVEE
                    ? Notification.TypeNotification.DEMANDE_CONGE_APPROUVEE
                    : Notification.TypeNotification.DEMANDE_CONGE_REJETEE;

            String titre = demande.getStatut() == StatutDemandeConge.APPROUVEE
                    ? "Demande de congé approuvée"
                    : "Demande de congé rejetée";

            String notifMessage = "Votre demande de congé du " + demande.getDateDebut() +
                    " au " + demande.getDateFin() + " a été " +
                    (demande.getStatut() == StatutDemandeConge.APPROUVEE ? "approuvée" : "rejetée");

            if (demande.getStatut() == StatutDemandeConge.REJETEE && demande.getMotifRejet() != null) {
                notifMessage += ". Motif : " + demande.getMotifRejet();
            }

            try {
                notificationService.creerNotification(
                        employeUser.getId(),
                        titre,
                        notifMessage,
                        type,
                        demande.getId(),
                        "DEMANDE_CONGE"
                );
            } catch (Exception e) {
                logger.error("❌ Erreur création notification interne: {}", e.getMessage());
            }
        }
    }

    // Envoie un rappel aux managers pour les demandes en attente depuis trop longtemps
    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    public void envoyerRappelsDemandesEnAttente() {
        if (!notificationsEnabled) return;

        logger.info("Exécution de la tâche planifiée: rappels des demandes en attente");

        int joursAttente = 3;
        LocalDate dateLimit = LocalDate.now().minusDays(joursAttente);

        List<DemandeConge> demandesEnAttente = demandeCongeRepository.findDemandesEnAttenteDepuisJours(
                StatutDemandeConge.EN_ATTENTE,
                dateLimit
        );

        logger.info("Nombre de demandes en attente depuis plus de {} jours: {}", joursAttente, demandesEnAttente.size());

        for (DemandeConge demande : demandesEnAttente) {
            try {
                User manager = getManagerEmploye(demande.getEmploye());
                if (manager == null) continue;

                Entreprise entreprise = demande.getEmploye().getEntreprise();
                String managerEmail = manager.getUsername();

                if (entreprise != null && entreprise.getEmailEntreprise() != null && !entreprise.getEmailEntreprise().isEmpty()) {
                    managerEmail = entreprise.getEmailEntreprise();
                }

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

                // ✅ CORRECTION : Try-Catch pour chaque rappel individuel
                try {
                    emailService.sendCongeDetailedNotification(
                            managerEmail,
                            sujet,
                            demande,
                            greeting,
                            message,
                            entreprise
                    );
                    logger.info("Rappel envoyé au manager (email: {})", managerEmail);
                } catch (Exception e) {
                    logger.error("Erreur envoi rappel email: {}", e.getMessage());
                }

            } catch (Exception e) {
                logger.error("Erreur lors du traitement du rappel pour la demande ID: {}", demande.getId(), e);
            }
        }
    }

    // Envoie un rappel aux employés la veille de leur retour de congé
    @Scheduled(cron = "0 0 16 * * *") // Tous les jours à 16h
    public void envoyerRappelsRetourCongé() {
        if (!notificationsEnabled) return;

        logger.info("Exécution de la tâche planifiée: rappels de retour de congé");

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

            // ✅ CORRECTION : Try-Catch pour l'email
            try {
                emailService.sendCongeDetailedNotification(
                        emailEmploye,
                        sujet,
                        demande,
                        greeting,
                        message,
                        entreprise
                );
                logger.info("Rappel de retour envoyé à l'employé: {}", emailEmploye);
            } catch (Exception e) {
                logger.error("Erreur envoi rappel retour: {}", e.getMessage());
            }
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

            // ✅ CORRECTION : Try-Catch
            try {
                emailService.sendCongeDetailedNotification(
                        emailEmploye,
                        sujet,
                        demande,
                        greeting,
                        message,
                        entreprise
                );
                logger.info("Notification d'annulation envoyée à l'employé: {}", emailEmploye);
            } catch (Exception e) {
                logger.error("Erreur envoi email annulation employé: {}", e.getMessage());
            }
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

            // ✅ CORRECTION : Try-Catch
            try {
                emailService.sendCongeDetailedNotification(
                        managerEmail,
                        sujet,
                        demande,
                        greeting,
                        message,
                        entreprise
                );
                logger.info("Notification d'annulation envoyée à l'employeur: {}", managerEmail);
            } catch (Exception e) {
                logger.error("Erreur envoi email annulation employeur: {}", e.getMessage());
            }
        }
    }

    private User getManagerEmploye(Employe employe) {
        if (employe == null || employe.getEntreprise() == null) {
            return null;
        }
        Long entrepriseId = employe.getEntreprise().getId();
        return userRepository.findFirstEmployeurByEntrepriseId(entrepriseId);
    }

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