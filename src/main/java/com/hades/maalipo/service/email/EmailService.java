package com.hades.maalipo.service.email;

import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Entreprise;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender emailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.frontend.password-reset-url}")
    private String passwordResetBaseUrl;

    // ✅ NOUVEAU : Configuration anti-spam
    @Value("${app.mail.from-name:Système Maalipo}")
    private String fromName;

    @Value("${app.mail.domain:ngombe.org}")
    private String mailDomain;

    @Autowired
    public EmailService(JavaMailSender emailSender, SpringTemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    // ✅ NOUVELLE méthode pour configurer les headers anti-spam
    private void configureAntiSpamHeaders(MimeMessageHelper helper, String subject) throws MessagingException  {
        try {
            // ✅ CORRECTION : Gestion de l'exception UnsupportedEncodingException
            helper.setFrom(fromEmail, fromName);
        } catch (UnsupportedEncodingException e) {
            // Fallback : utiliser seulement l'email sans le nom si encodage échoue
            helper.setFrom(fromEmail);
            System.err.println("⚠️ Impossible d'encoder le nom d'expéditeur, utilisation de l'email seul: " + e.getMessage());
        }

        // Headers anti-spam
        helper.getMimeMessage().setHeader("X-Mailer", "Maalipo-System");
        helper.getMimeMessage().setHeader("X-Priority", "3");
        helper.getMimeMessage().setHeader("X-MSMail-Priority", "Normal");
        helper.getMimeMessage().setHeader("Return-Path", fromEmail);
        helper.getMimeMessage().setHeader("Reply-To", fromEmail);
        helper.getMimeMessage().setHeader("Organization", "Maalipo - Système de gestion RH");

        // Message-ID personnalisé
        String messageId = System.currentTimeMillis() + "@" + mailDomain;
        helper.getMimeMessage().setHeader("Message-ID", "<" + messageId + ">");

        // Classification du contenu
        helper.getMimeMessage().setHeader("Content-Type", "text/html; charset=UTF-8");
        helper.getMimeMessage().setHeader("MIME-Version", "1.0");
    }

    // ✅ AMÉLIORATION : Notification de congé
    public void sendCongeNotification(String to, String subject, String messageContent) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Maalipo] " + subject); // ✅ Préfixe professionnel

            // ✅ Configuration anti-spam
            configureAntiSpamHeaders(helper, subject);

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", "Bonjour,");
            context.setVariable("emailType", "general");
            context.setVariable("messageContent", messageContent);
            context.setVariable("logoUrl", "https://maalipo.ngombe.org/assets/logo.png"); // ✅ URL réelle
            context.setVariable("companyName", "Maalipo");
            context.setVariable("systemUrl", "https://maalipo.ngombe.org");

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            System.out.println("✅ Email envoyé avec succès à: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email à " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ AMÉLIORATION : Notification détaillée pour une demande de congé
    public void sendCongeDetailedNotification(String to, String subject, DemandeConge demande,
                                              String greeting, String message, Entreprise entreprise) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Maalipo] " + subject);

            // ✅ Configuration anti-spam
            configureAntiSpamHeaders(helper, subject);

            // Formateur de date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", greeting);
            context.setVariable("emailType", "conge-notification");
            context.setVariable("messageContent", message);
            context.setVariable("logoUrl", "https://maalipo.ngombe.org/assets/logo.png");
            context.setVariable("companyName", "Maalipo");
            context.setVariable("systemUrl", "https://maalipo.ngombe.org");

            // Détails du congé
            Map<String, String> congeDetails = new HashMap<>();
            congeDetails.put("type", demande.getTypeConge().getLibelle());
            congeDetails.put("statut", demande.getStatut().toString());
            congeDetails.put("dateDebut", demande.getDateDebut().format(dateFormatter));
            congeDetails.put("dateFin", demande.getDateFin().format(dateFormatter));

            context.setVariable("congeDetails", congeDetails);

            // Informations de l'entreprise si disponibles
            if (entreprise != null) {
                context.setVariable("companyAddress", entreprise.getAdresseEntreprise());
                context.setVariable("companyPhone", entreprise.getTelephoneEntreprise());
                context.setVariable("companyEmail", entreprise.getEmailEntreprise());
                context.setVariable("companyName", entreprise.getNom());
            }

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(mimeMessage);
            System.out.println("✅ Email de congé envoyé avec succès à: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de congé à " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ AMÉLIORATION : Email de réinitialisation de mot de passe
    public void sendPasswordResetEmail(String to, String token, String fullName) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Maalipo] Réinitialisation de votre mot de passe");

            // ✅ Configuration anti-spam
            configureAntiSpamHeaders(helper, "Réinitialisation de mot de passe");

            // Construction du lien de réinitialisation
            String resetLink = passwordResetBaseUrl + "?token=" + token;

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", "Réinitialisation de votre mot de passe");
            context.setVariable("greeting", "Bonjour " + fullName + ",");
            context.setVariable("emailType", "password-reset");
            context.setVariable("resetLink", resetLink);
            context.setVariable("logoUrl", "https://maalipo.ngombe.org/assets/logo.png");
            context.setVariable("companyName", "Maalipo");
            context.setVariable("systemUrl", "https://maalipo.ngombe.org");
            context.setVariable("fullName", fullName);

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            System.out.println("✅ Email de réinitialisation envoyé avec succès à: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de réinitialisation à " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ AMÉLIORATION : Notification générale
    public void sendGeneralNotification(String to, String subject, String greeting,
                                        String messageContent, Entreprise entreprise) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Maalipo] " + subject);

            // ✅ Configuration anti-spam
            configureAntiSpamHeaders(helper, subject);

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", greeting);
            context.setVariable("emailType", "general");
            context.setVariable("messageContent", messageContent);
            context.setVariable("logoUrl", "https://maalipo.ngombe.org/assets/logo.png");
            context.setVariable("companyName", "Maalipo");
            context.setVariable("systemUrl", "https://maalipo.ngombe.org");

            // Informations de l'entreprise si disponibles
            if (entreprise != null) {
                context.setVariable("companyAddress", entreprise.getAdresseEntreprise());
                context.setVariable("companyPhone", entreprise.getTelephoneEntreprise());
                context.setVariable("companyEmail", entreprise.getEmailEntreprise());
                context.setVariable("companyName", entreprise.getNom());
            }

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            System.out.println("✅ Email général envoyé avec succès à: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email général à " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    //  Méthode pour envoyer notification de bulletin de paie
    public void sendBulletinPaieNotification(String to, String employeeName, String periode, String montant) {
        String subject = "Votre bulletin de paie est disponible - " + periode;
        String greeting = "Bonjour " + employeeName + ",";
        String messageContent = String.format(
                "Votre bulletin de paie pour la période %s est maintenant disponible dans votre espace personnel.\n\n" +
                        "Salaire net à payer : %s FCFA\n\n" +
                        "Vous pouvez le consulter et le télécharger en vous connectant à votre compte Maalipo.",
                periode, montant
        );

        sendGeneralNotification(to, subject, greeting, messageContent, null);
    }

    public void sendBulletinPaieNotification(String to, String employeeName, String periode,
                                             String montantNet, String entrepriseName,
                                             String bulletinUrl, Entreprise entreprise) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[Maalipo] 📄 Votre bulletin de paie " + periode + " est disponible");

            // Configuration anti-spam
            configureAntiSpamHeaders(helper, "Bulletin de paie disponible");

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", "Bulletin de paie disponible - " + periode);
            context.setVariable("greeting", "Bonjour " + employeeName + ",");
            context.setVariable("emailType", "bulletin-paie");
            context.setVariable("logoUrl", "https://maalipo.ngombe.org/assets/logo.png");
            context.setVariable("companyName", "Maalipo");
            context.setVariable("systemUrl", "https://maalipo.ngombe.org");

            // ✅ Variables spécifiques au bulletin de paie
            context.setVariable("employeeName", employeeName);
            context.setVariable("periode", periode);
            context.setVariable("montantNet", montantNet);
            context.setVariable("entrepriseName", entrepriseName);
            context.setVariable("bulletinUrl", bulletinUrl);

            // Informations de l'entreprise si disponibles
            if (entreprise != null) {
                context.setVariable("companyAddress", entreprise.getAdresseEntreprise());
                context.setVariable("companyPhone", entreprise.getTelephoneEntreprise());
                context.setVariable("companyEmail", entreprise.getEmailEntreprise());
                context.setVariable("companyName", entreprise.getNom());
            }

            // Message personnalisé pour le bulletin
            String messageContent = String.format(
                    "Votre bulletin de paie pour la période <strong>%s</strong> vient d'être généré et est maintenant disponible dans votre espace personnel Maalipo.\n\n" +
                            "Vous pouvez dès maintenant le consulter, le télécharger au format PDF et l'imprimer selon vos besoins.",
                    periode
            );
            context.setVariable("messageContent", messageContent);

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            System.out.println("✅ Email de bulletin de paie envoyé avec succès à: " + to + " pour la période: " + periode);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de bulletin à " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

}