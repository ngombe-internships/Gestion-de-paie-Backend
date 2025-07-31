package com.hades.paie1.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService (JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to , String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Réinitialisation de Votre mot de passe pour PayCal.\n ");
            message.setText("Bonjour,\n\n"
                    + "Vous avez demandé à réinitialiser votre mot de passe pour PaieApp.\n"
                    + "Veuillez cliquer sur le lien ci-dessous pour continuer :\n"
                    + resetLink + "\n\n"
                    + "Ce lien est valide pour 1 heures.\n"
                    + "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet e-mail.\n\n"
                    + "Cordialement,\n"
                    + "L'équipe PayCal");
            mailSender.send(message);
            System.out.println("Email de réinitialisation envoyé a : " + to);
        } catch (MailException e) {
            System.out.println("Erreur lors de l'envoie de l'email de réinitialisation á " + to + ": " +e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

}
