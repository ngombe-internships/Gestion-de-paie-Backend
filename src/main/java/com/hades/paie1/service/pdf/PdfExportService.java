//package com.hades.paie1.service.pdf;
//
//import com.hades.paie1.dto.AuditLogDto;
//import org.apache.pdfbox.pdmodel.*;
//import org.apache.pdfbox.pdmodel.common.PDRectangle;
//import org.apache.pdfbox.pdmodel.font.PDType1Font;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.List;
//
//@Service
//public class PdfExportService {
//
//    public byte[] exportAuditLogsToPdf(List<AuditLogDto> logs, String titre, String periode) throws IOException {
//        PDDocument document = new PDDocument();
//        PDPage page = new PDPage(PDRectangle.A4);
//        document.addPage(page);
//
//        PDPageContentStream contentStream = new PDPageContentStream(document, page);
//
//        int margin = 50;
//        int yStart = (int) page.getMediaBox().getHeight() - margin;
//
//        // Titre
//        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
//        contentStream.beginText();
//        contentStream.newLineAtOffset(margin, yStart);
//        contentStream.showText(titre);
//        contentStream.endText();
//
//        contentStream.setFont(PDType1Font.HELVETICA, 12);
//        contentStream.beginText();
//        contentStream.newLineAtOffset(margin, yStart - 30);
//        contentStream.showText("Période : " + periode);
//        contentStream.endText();
//
//        // Table header
//        int y = yStart - 60;
//        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
//        contentStream.beginText();
//        contentStream.newLineAtOffset(margin, y);
//        contentStream.showText("Date       | Utilisateur | Action | Détail");
//        contentStream.endText();
//
//        contentStream.setFont(PDType1Font.HELVETICA, 9);
//
//        // Table rows
//        for (AuditLogDto log : logs) {
//            y -= 18;
//            if (y < 50) break; // simple gestion de page unique, à améliorer si besoin
//            contentStream.beginText();
//            contentStream.newLineAtOffset(margin, y);
//            String line = String.format("%s | %s | %s | %s",
//                    log.getDateAction(), log.getUsername(), log.getAction(), log.getDetails());
//            contentStream.showText(line);
//            contentStream.endText();
//        }
//
//        contentStream.close();
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        document.save(out);
//        document.close();
//        return out.toByteArray();
//    }
//}