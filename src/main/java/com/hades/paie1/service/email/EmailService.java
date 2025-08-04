package com.hades.paie1.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            logger.info("Tentative d'envoi d'email de réinitialisation à : {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Réinitialisation de Votre mot de passe pour Maalipo");

            String emailBody = buildEmailBody(resetLink);
            message.setText(emailBody);

            mailSender.send(message);
            logger.info("Email de réinitialisation envoyé avec succès à : {}", to);

        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de réinitialisation à {} : {}", to, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de réinitialisation", e);
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'envoi de l'email à {} : {}", to, e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de l'envoi de l'email", e);
        }
    }

    private String buildEmailBody(String resetLink) {
        return "Bonjour,\n\n" +
                "Vous avez demandé à réinitialiser votre mot de passe pour Maalipo.\n" +
                "Veuillez cliquer sur le lien ci-dessous pour continuer :\n\n" +
                resetLink + "\n\n" +
                "Ce lien est valide pour 1 heure.\n" +
                "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet e-mail.\n\n" +
                "Cordialement,\n" +
                "L'équipe Maalipo";
    }

}