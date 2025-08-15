package com.hades.maalipo.controller;

import com.hades.maalipo.dto.authen.AuditLogDto;
import com.hades.maalipo.dto.bulletin.PdfExportRequestDto;
import com.hades.maalipo.service.AuditLogService;

import com.hades.maalipo.service.email.MailLogService;
import com.hades.maalipo.service.pdf.PdfExportService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private PdfExportService pdfExportService;
    @Autowired
    private MailLogService mailService;


    @GetMapping
    public Page<AuditLogDto> getAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return auditLogService.getAuditLogs(username, entityName, entityId, action, page, size);
    }


    @PostMapping("/pdf")
    public ResponseEntity<byte[]> exportAuditLogsPdf(@RequestBody PdfExportRequestDto request)
            throws IOException, MessagingException {

        // Convertir les strings en LocalDateTime
        LocalDateTime from = LocalDateTime.parse(request.getFrom() + "T00:00:00");
        LocalDateTime to = LocalDateTime.parse(request.getTo() + "T23:59:59");

        // Récupère tous les logs dans la plage
        List<AuditLogDto> logs = auditLogService.getAuditLogsByDateRange(from, to);

        // 🎯 AMÉLIORATION : Formatage propre de la période
        String periode = pdfExportService.formatPeriodForTitle(from, to);

        byte[] pdf = pdfExportService.exportAuditLogsToPdf(logs, "Rapport d'audit", periode);

        String fileName = pdfExportService.generateSmartFileName(from, to);

        // Si un email est fourni, envoyer par email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String emailSubject = String.format("Rapport d'audit du %s au %s",
                    from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            mailService.sendAuditLogPdf(request.getEmail(), pdf, emailSubject);
        }

        // Retourner le PDF pour téléchargement
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }


}
