package com.hades.maalipo.service.email;


import com.hades.maalipo.dto.authen.AuditLogDto;
import com.hades.maalipo.service.AuditLogService;
import com.hades.maalipo.service.pdf.PdfExportService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogSchedulerService {

    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private PdfExportService pdfExportService;
    @Autowired
    private MailLogService mailService;

    // export automatique chaque dimanche a 8h
    @Scheduled(cron = "0 0 8 * * SUN")
    public void exportAndSendDailyAuditLogs() throws IOException, MessagingException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(7);
        List<AuditLogDto> logs = auditLogService.getAuditLogsByDateRange(yesterday, now);

        byte[] pdf = pdfExportService.exportAuditLogsToPdf(logs, "Rapport d'audit quotidien", yesterday + " à " + now);
        mailService.sendAuditLogPdf("maalipo@ngombe.org", pdf, "Rapport d'audit quotidien PDF");
    }
}