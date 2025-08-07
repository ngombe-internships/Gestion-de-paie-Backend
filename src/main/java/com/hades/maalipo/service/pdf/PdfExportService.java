package com.hades.maalipo.service.pdf;

import com.hades.maalipo.dto.AuditLogDto;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExportService {

    private static final int MARGIN = 50;
    private static final int BASE_LINE_HEIGHT = 20;
    private static final int ITEMS_PER_PAGE = 30; // Réduit pour permettre les lignes multiples
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm");

    // Largeurs des colonnes
    private static final int DATE_WIDTH = 100;
    private static final int USER_WIDTH = 80;
    private static final int ACTION_WIDTH = 120;
    private static final int DETAILS_WIDTH = 200;

    /**
     * Génère le nom de fichier dynamique
     */
    public String generateFileName() {
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        return "rapport-audit-logs-" + timestamp + ".pdf";
    }

    public byte[] exportAuditLogsToPdf(List<AuditLogDto> logs, String titre, String periode) throws IOException {
        PDDocument document = new PDDocument();

        try {
            int totalPages = calculateTotalPages(logs);
            int currentLogIndex = 0;

            for (int pageNum = 0; pageNum < Math.max(1, totalPages); pageNum++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    int yPosition = createPageHeader(contentStream, page, titre, periode, pageNum + 1, totalPages);

                    if (pageNum == 0) {
                        yPosition = createTableHeader(contentStream, yPosition);
                    } else {
                        yPosition -= 20;
                        yPosition = createTableHeader(contentStream, yPosition);
                    }

                    // Ajouter les logs avec gestion des détails multilignes
                    LogProcessingResult result = addLogsToPage(contentStream, logs, currentLogIndex, yPosition);
                    currentLogIndex = result.nextLogIndex;

                    // Pied de page
                    createPageFooter(contentStream, page, logs.size(), pageNum + 1, totalPages);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } finally {
            document.close();
        }
    }

    /**
     * Calcule le nombre total de pages nécessaires en tenant compte des détails multilignes
     */
    private int calculateTotalPages(List<AuditLogDto> logs) {
        int totalLines = 0;
        for (AuditLogDto log : logs) {
            // Chaque log prend au moins 1 ligne, plus les lignes supplémentaires pour les détails
            int detailLines = calculateDetailLines(log.getDetails());
            totalLines += Math.max(1, detailLines);
        }
        return (int) Math.ceil((double) totalLines / ITEMS_PER_PAGE);
    }

    /**
     * Calcule combien de lignes sont nécessaires pour afficher les détails
     */
    private int calculateDetailLines(String details) {
        if (details == null || details.isEmpty()) return 1;

        // Estimation : environ 45 caractères par ligne dans la colonne détails
        int maxCharsPerLine = 45;
        return (int) Math.ceil((double) details.length() / maxCharsPerLine);
    }

    /**
     * Classe pour retourner le résultat du traitement des logs
     */
    private static class LogProcessingResult {
        int nextLogIndex;
        int finalYPosition;

        LogProcessingResult(int nextLogIndex, int finalYPosition) {
            this.nextLogIndex = nextLogIndex;
            this.finalYPosition = finalYPosition;
        }
    }

    /**
     * Ajoute les logs à une page en gérant les détails multilignes
     */
    private LogProcessingResult addLogsToPage(PDPageContentStream contentStream, List<AuditLogDto> logs,
                                              int startIndex, int startYPosition) throws IOException {
        int yPosition = startYPosition;
        int logIndex = startIndex;
        int rowCount = 0;

        while (logIndex < logs.size() && rowCount < ITEMS_PER_PAGE) {
            AuditLogDto log = logs.get(logIndex);

            // Vérifier s'il y a assez d'espace pour au moins une ligne
            if (yPosition < MARGIN + 100) {
                break;
            }

            // Ajouter la ligne avec détails complets
            int linesUsed = addLogRowWithFullDetails(contentStream, log, yPosition, rowCount % 2 == 0);
            yPosition -= (linesUsed * BASE_LINE_HEIGHT);

            logIndex++;
            rowCount += linesUsed;
        }

        return new LogProcessingResult(logIndex, yPosition);
    }

    /**
     * Ajoute une ligne de log avec les détails complets (multilignes si nécessaire)
     */
    private int addLogRowWithFullDetails(PDPageContentStream contentStream, AuditLogDto log,
                                         int yPosition, boolean alternateRow) throws IOException {

        // Préparer les données
        String dateStr = formatLogDate(String.valueOf(log.getDateAction()));
        String actionStr = getActionLabel(log.getAction());
        String usernameStr = truncateText(log.getUsername(), 12);

        // Diviser les détails en lignes
        List<String> detailLines = splitTextIntoLines(log.getDetails(), 45);
        int totalLines = Math.max(1, detailLines.size());

        float tableWidth = DATE_WIDTH + USER_WIDTH + ACTION_WIDTH + DETAILS_WIDTH;
        int rowHeight = totalLines * BASE_LINE_HEIGHT;

        // Fond alterné pour toute la ligne (même multiligne)
        if (alternateRow) {
            contentStream.setNonStrokingColor(0.98f, 0.98f, 0.98f);
            contentStream.addRect(MARGIN, yPosition - rowHeight + 5, tableWidth, rowHeight);
            contentStream.fill();
        }

        contentStream.setNonStrokingColor(0f, 0f, 0f);
        contentStream.setFont(PDType1Font.HELVETICA, 8);

        // Première ligne avec toutes les infos de base
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 2, yPosition - 10);
        contentStream.showText(dateStr);
        contentStream.newLineAtOffset(DATE_WIDTH, 0);
        contentStream.showText(usernameStr);
        contentStream.newLineAtOffset(USER_WIDTH, 0);
        contentStream.showText(actionStr);
        contentStream.newLineAtOffset(ACTION_WIDTH, 0);

        // Première ligne des détails
        if (!detailLines.isEmpty()) {
            contentStream.showText(detailLines.get(0));
        }
        contentStream.endText();

        // Lignes supplémentaires pour les détails si nécessaire
        for (int i = 1; i < detailLines.size(); i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + DATE_WIDTH + USER_WIDTH + ACTION_WIDTH + 2,
                    yPosition - 10 - (i * BASE_LINE_HEIGHT));
            contentStream.showText(detailLines.get(i));
            contentStream.endText();
        }

        // Bordure de la ligne complète
        contentStream.setStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(MARGIN, yPosition - rowHeight + 5, tableWidth, rowHeight);
        contentStream.stroke();

        return totalLines;
    }

    /**
     * Divise un texte long en plusieurs lignes
     */
    private List<String> splitTextIntoLines(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        // Diviser le texte en mots
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Si ajouter ce mot dépasse la limite
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        // Ajouter la dernière ligne
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.isEmpty() ? List.of("") : lines;
    }

    private int createPageHeader(PDPageContentStream contentStream, PDPage page, String titre,
                                 String periode, int currentPage, int totalPages) throws IOException {
        int yStart = (int) page.getMediaBox().getHeight() - MARGIN;

        // Titre principal
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yStart);
        contentStream.showText(titre);
        contentStream.endText();

        // Période
        contentStream.setFont(PDType1Font.HELVETICA, 11);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yStart - 25);
        contentStream.showText("Période : " + formatPeriod(periode));
        contentStream.endText();

        // Date de génération
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yStart - 45);
        contentStream.showText("Généré le : " + LocalDateTime.now().format(DATE_FORMATTER));
        contentStream.endText();

        // Numéro de page
        if (totalPages > 1) {
            contentStream.beginText();
            contentStream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN - 100, yStart);
            contentStream.showText("Page " + currentPage + "/" + totalPages);
            contentStream.endText();
        }

        // Ligne de séparation
        contentStream.moveTo(MARGIN, yStart - 60);
        contentStream.lineTo(page.getMediaBox().getWidth() - MARGIN, yStart - 60);
        contentStream.stroke();

        return yStart - 80;
    }

    private int createTableHeader(PDPageContentStream contentStream, int yPosition) throws IOException {
        float tableWidth = DATE_WIDTH + USER_WIDTH + ACTION_WIDTH + DETAILS_WIDTH;

        // Fond gris pour l'en-tête
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(MARGIN, yPosition - 15, tableWidth, 18);
        contentStream.fill();

        // Texte d'en-tête
        contentStream.setNonStrokingColor(0f, 0f, 0f);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 5, yPosition - 10);
        contentStream.showText("Date & Heure");
        contentStream.newLineAtOffset(DATE_WIDTH, 0);
        contentStream.showText("Utilisateur");
        contentStream.newLineAtOffset(USER_WIDTH, 0);
        contentStream.showText("Action");
        contentStream.newLineAtOffset(ACTION_WIDTH, 0);
        contentStream.showText("Détails");
        contentStream.endText();

        // Bordures de l'en-tête
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.addRect(MARGIN, yPosition - 15, tableWidth, 18);
        contentStream.stroke();

        return yPosition - 25;
    }

    private void createPageFooter(PDPageContentStream contentStream, PDPage page, int totalLogs,
                                  int currentPage, int totalPages) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA, 8);
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);

        // Ligne de séparation
        contentStream.moveTo(MARGIN, 40);
        contentStream.lineTo(page.getMediaBox().getWidth() - MARGIN, 40);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, 25);
        contentStream.showText("Total des logs : " + totalLogs + " entrées");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(page.getMediaBox().getWidth() / 2 - 30, 25);
        contentStream.showText("Page " + currentPage + "/" + totalPages);
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN - 150, 25);
        contentStream.showText("Document généré automatiquement");
        contentStream.endText();
    }

    // Méthodes utilitaires inchangées
    private String formatPeriod(String periode) {
        try {
            return periode.replace("T00:00", "")
                    .replace("T23:59:59", "")
                    .replace(" à ", " au ");
        } catch (Exception e) {
            return periode;
        }
    }

    private String formatLogDate(String dateStr) {
        try {
            if (dateStr.contains("T")) {
                String[] parts = dateStr.split("T");
                String datePart = parts[0];
                String timePart = parts[1].substring(0, 8);
                return datePart + " " + timePart;
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String getActionLabel(String action) {
        switch (action) {
            case "CREATE_EMPLOYE_PAIE_CONFIG": return "Création config paie";
            case "DELETE_EMPLOYE_PAIE_CONFIG": return "Suppression config paie";
            case "GENERATE_BULLETIN_PAIE": return "Génération bulletin";
            case "ACTIVATE_ENTREPRISE": return "Activation entreprise";
            case "DEACTIVATE_ENTREPRISE": return "Désactivation entreprise";
            case "LOGIN": return "Connexion";
            case "LOGOUT": return "Déconnexion";
            case "CREATE_USER": return "Création utilisateur";
            case "UPDATE_USER": return "Modification utilisateur";
            case "DELETE_USER": return "Suppression utilisateur";
            default: return action;
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }


    public String formatPeriodForTitle(LocalDateTime from, LocalDateTime to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return from.format(formatter) + " au " + to.format(formatter);
    }

    public String generateSmartFileName(LocalDateTime from, LocalDateTime to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format("audit-logs-%s-au-%s.pdf",
                from.format(formatter),
                to.format(formatter));
    }
}