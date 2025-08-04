//package com.hades.paie1.service.email;
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//
//@Service
//public class MailLogService {
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//    public void sendAuditLogPdf(String to, byte[] pdfBytes, String subject) throws MessagingException {
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//        helper.setTo(to);
//        helper.setSubject(subject);
//        helper.setText("Veuillez trouver ci-joint le rapport d'audit exporté en PDF.");
//        helper.addAttachment("audit-logs.pdf", new ByteArrayResource(pdfBytes));
//
//        mailSender.send(message);
//    }
//}