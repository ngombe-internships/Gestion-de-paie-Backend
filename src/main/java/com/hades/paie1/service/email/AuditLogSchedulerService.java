//package com.hades.paie1.service.email;
//
//
//import com.hades.paie1.dto.AuditLogDto;
//import com.hades.paie1.service.AuditLogService;
//import com.hades.paie1.service.pdf.PdfExportService;
//import jakarta.mail.MessagingException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//public class AuditLogSchedulerService {
//
//    @Autowired
//    private AuditLogService auditLogService;
//    @Autowired
//    private PdfExportService pdfExportService;
//    @Autowired
//    private MailLogService mailService;
//
//    // Tous les 10 min
//    @Scheduled(cron = "0 */10 * * * ?")
//    public void exportAndSendDailyAuditLogs() throws IOException, MessagingException {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime yesterday = now.minusDays(1);
//        List<AuditLogDto> logs = auditLogService.getAuditLogsByDateRange(yesterday, now);
//
//        byte[] pdf = pdfExportService.exportAuditLogsToPdf(logs, "Rapport d'audit quotidien", yesterday + " à " + now);
//        mailService.sendAuditLogPdf("jackeboyeur244@gmail.com", pdf, "Rapport d'audit quotidien PDF");
//    }
//}