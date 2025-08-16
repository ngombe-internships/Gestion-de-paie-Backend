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

    @Value("${app.mail.from-name:Système Maalipo}")
    private String fromName;

    @Autowired
    public EmailService(JavaMailSender emailSender, SpringTemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    // Envoie une notification de congé en utilisant le template HTML

    public void sendCongeNotification(String to, String subject, String messageContent) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", "Bonjour,");
            context.setVariable("emailType", "general");
            context.setVariable("messageContent", messageContent);
            context.setVariable("logoUrl", "https://via.placeholder.com/120x60?text=Maalipo");

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Envoie une notification détaillée pour une demande de congé

    public void sendCongeDetailedNotification(String to, String subject, DemandeConge demande,
                                              String greeting, String message, Entreprise entreprise) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Formateur de date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", greeting);
            context.setVariable("emailType", "conge-notification");
            context.setVariable("messageContent", message);
            context.setVariable("logoUrl", "https://via.placeholder.com/120x60?text=Maalipo");

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
            }

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Envoie un email de réinitialisation de mot de passe

    public void sendPasswordResetEmail(String to, String token, String fullName) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Maalipo - Réinitialisation de mot de passe");

            // Construction du lien de réinitialisation
            String resetLink = passwordResetBaseUrl + "?token=" + token;

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", "Réinitialisation de mot de passe");
            context.setVariable("greeting", "Bonjour " + fullName + ",");
            context.setVariable("emailType", "password-reset");
            context.setVariable("resetLink", resetLink);
            context.setVariable("logoUrl", "https://via.placeholder.com/120x60?text=Maalipo");

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);

            System.out.println("Email de réinitialisation envoyé à: " + to + " avec lien: " + resetLink);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //Envoie une notification générale (pour les autres types de messages)
    public void sendGeneralNotification(String to, String subject, String greeting,
                                        String messageContent, Entreprise entreprise) {
        if (!emailEnabled) {
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Préparation du contexte pour le template
            Context context = new Context(Locale.FRENCH);
            context.setVariable("emailSubject", subject);
            context.setVariable("greeting", greeting);
            context.setVariable("emailType", "general");
            context.setVariable("messageContent", messageContent);
            context.setVariable("logoUrl", "https://via.placeholder.com/120x60?text=Maalipo");

            // Informations de l'entreprise si disponibles
            if (entreprise != null) {
                context.setVariable("companyAddress", entreprise.getAdresseEntreprise());
                context.setVariable("companyPhone", entreprise.getTelephoneEntreprise());
                context.setVariable("companyEmail", entreprise.getEmailEntreprise());
            }

            // Génération du contenu HTML à partir du template
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}