package com.hades.paie1.service.pdf;

import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.dto.LignePaieDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final TemplateEngine templateEngine;
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Méthode centralisée pour gérer les URLs de logo selon le contexte
     */
    private String processLogoUrl(String originalUrl, boolean isForPdf) {
        if (originalUrl == null || originalUrl.isEmpty()) {
            return originalUrl;
        }

        logger.debug("Traitement de l'URL logo: {} (PDF: {})", originalUrl, isForPdf);

        // URL Cloudinary (production) - pas de modification nécessaire
        if (originalUrl.startsWith("https://res.cloudinary.com")) {
            logger.info("URL Cloudinary utilisée: {}", originalUrl);
            return originalUrl;
        }

        // Chemin local commençant par /logos/
        if (originalUrl.startsWith("/logos/")) {
            String fileName = originalUrl.substring(originalUrl.lastIndexOf("/") + 1);

            if (isForPdf) {
                // Pour PDF: utiliser file:// URL
                Path absoluteLogoPath = Paths.get(uploadDir).resolve(fileName);
                String fileUrl = absoluteLogoPath.toUri().toString();
                logger.info("URL file:// générée pour PDF: {}", fileUrl);
                return fileUrl;
            } else {
                // Pour HTML preview: utiliser URL HTTP locale
                String httpUrl = "http://localhost:8081/logos/" + fileName;
                logger.info("URL HTTP générée pour preview: {}", httpUrl);
                return httpUrl;
            }
        }

        // URL file:// déjà formattée (cas de re-traitement)
        if (originalUrl.startsWith("file://")) {
            if (isForPdf) {
                return originalUrl; // Déjà au bon format pour PDF
            } else {
                // Convertir vers HTTP pour preview
                String fileName = originalUrl.substring(originalUrl.lastIndexOf("/") + 1);
                String httpUrl = "http://localhost:8081/logos/" + fileName;
                logger.info("Conversion file:// vers HTTP pour preview: {}", httpUrl);
                return httpUrl;
            }
        }

        // URLs HTTP/HTTPS complètes - pas de modification
        if (originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) {
            return originalUrl;
        }

        // Chemin relatif sans préfixe
        if (isForPdf) {
            Path absoluteLogoPath = Paths.get(uploadDir).resolve(originalUrl);
            String fileUrl = absoluteLogoPath.toUri().toString();
            logger.info("URL file:// générée pour chemin relatif (PDF): {}", fileUrl);
            return fileUrl;
        } else {
            String httpUrl = "http://localhost:8081/logos/" + originalUrl;
            logger.info("URL HTTP générée pour chemin relatif (preview): {}", httpUrl);
            return httpUrl;
        }
    }

    public byte[] generateBulletinPdf(BulletinPaieResponseDto bulletinData) throws IOException {
        try {
            // Cloner les données pour éviter de modifier l'original
            BulletinPaieResponseDto pdfData = cloneBulletinData(bulletinData);

            // Traiter l'URL du logo pour la génération PDF
            if (pdfData.getEntreprise() != null && pdfData.getEntreprise().getLogoUrl() != null) {
                String processedLogoUrl = processLogoUrl(pdfData.getEntreprise().getLogoUrl(), true);
                pdfData.getEntreprise().setLogoUrl(processedLogoUrl);
            }

            Context context = new Context();
            context.setVariable("bulletin", pdfData);

            String html = templateEngine.process("bulletin-template", context);

            // Parsing et nettoyage du HTML avec JSoup
            Document document = Jsoup.parse(html);
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            String cleanHtml = document.html();

            // Génération du PDF avec OpenHTMLToPDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(cleanHtml, null);
            builder.toStream(outputStream);

            // Configuration selon l'environnement
            String logoUrl = pdfData.getEntreprise() != null ? pdfData.getEntreprise().getLogoUrl() : null;

            if (logoUrl != null && logoUrl.startsWith("file://")) {
                // Environnement de développement - utiliser CustomStreamFactory
                String baseUriForRelativeResources = Paths.get(uploadDir).getParent().toUri().toString();
                builder.withUri(baseUriForRelativeResources);
                builder.useProtocolsStreamImplementation(new CustomStreamFactory(), "file");
                logger.info("Configuration PDF pour développement (file://) : {}", baseUriForRelativeResources);
            } else {
                // Environnement de production - OpenHTMLToPDF accède directement aux URLs HTTPS
                logger.info("Configuration PDF pour production (HTTPS Cloudinary)");
            }

            builder.run();
            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF : {}", e.getMessage(), e);
            throw new IOException("Erreur lors de la génération du PDF", e);
        }
    }

    public String generateHtmlContentForPdf(BulletinPaieResponseDto bulletinData) {
        // Cloner les données pour éviter de modifier l'original
        BulletinPaieResponseDto htmlData = cloneBulletinData(bulletinData);

        // Traiter l'URL du logo pour la prévisualisation HTML
        if (htmlData.getEntreprise() != null && htmlData.getEntreprise().getLogoUrl() != null) {
            String processedLogoUrl = processLogoUrl(htmlData.getEntreprise().getLogoUrl(), false);
            htmlData.getEntreprise().setLogoUrl(processedLogoUrl);
        }

        Context context = new Context();
        context.setVariable("bulletin", htmlData);
        context.setVariable("isPreview", true);

        return templateEngine.process("bulletin-template", context);
    }

    // Méthode améliorée de clonage avec logging
    private BulletinPaieResponseDto cloneBulletinData(BulletinPaieResponseDto original) {
        logger.debug("Clonage des données du bulletin ID: {}", original.getId());

        BulletinPaieResponseDto clone = new BulletinPaieResponseDto();

        // Copier tous les champs primitifs et String
        clone.setId(original.getId());
        clone.setTauxHoraire(original.getTauxHoraire());
        clone.setHeuresNormal(original.getHeuresNormal());
        clone.setSalaireBrut(original.getSalaireBrut());
        clone.setSalaireImposable(original.getSalaireImposable());
        clone.setBaseCnps(original.getBaseCnps());
        clone.setCoutTotalEmployeur(original.getCoutTotalEmployeur());
        clone.setCotisationCnps(original.getCotisationCnps());
        clone.setPeriodePaie(original.getPeriodePaie());
        clone.setDateCreationBulletin(original.getDateCreationBulletin());
        clone.setStatusBulletin(original.getStatusBulletin());
        clone.setDatePaiement(original.getDatePaiement());
        clone.setMethodePaiement(original.getMethodePaiement());

        // Cloner l'employé
        if (original.getEmploye() != null) {
            clone.setEmploye(original.getEmploye());
        }

        // Cloner l'entreprise (deep copy pour éviter les modifications)
        if (original.getEntreprise() != null) {
            EntrepriseDto clonedEntreprise = new EntrepriseDto();
            clonedEntreprise.setId(original.getEntreprise().getId());
            clonedEntreprise.setNom(original.getEntreprise().getNom());
            clonedEntreprise.setNumeroSiret(original.getEntreprise().getNumeroSiret());
            clonedEntreprise.setAdresseEntreprise(original.getEntreprise().getAdresseEntreprise());
            clonedEntreprise.setTelephoneEntreprise(original.getEntreprise().getTelephoneEntreprise());
            clonedEntreprise.setEmailEntreprise(original.getEntreprise().getEmailEntreprise());
            clonedEntreprise.setLogoUrl(original.getEntreprise().getLogoUrl());
            clone.setEntreprise(clonedEntreprise);
        }

        // Cloner les lignes de paie
        if (original.getLignesPaie() != null) {
            List<LignePaieDto> lignesPaieClonees = original.getLignesPaie().stream()
                    .map(this::cloneLignePaieDto)
                    .collect(Collectors.toList());
            clone.setLignesPaie(lignesPaieClonees);
        }

        logger.debug("Clonage terminé pour le bulletin ID: {}", clone.getId());
        return clone;
    }

    private LignePaieDto cloneLignePaieDto(LignePaieDto original) {
        return LignePaieDto.builder()
                .designation(original.getDesignation())
                .categorie(original.getCategorie())
                .type(original.getType())
                .nombre(original.getNombre())
                .tauxApplique(original.getTauxApplique())
                .montantFinal(original.getMontantFinal())
                .baseApplique(original.getBaseApplique())
                .formuleCalcul(original.getFormuleCalcul())
                .build();
    }

    // Classes internes inchangées
    private static class CustomStreamFactory implements com.openhtmltopdf.extend.FSStreamFactory {
        private static final Logger logger = LoggerFactory.getLogger(CustomStreamFactory.class);

        @Override
        public com.openhtmltopdf.extend.FSStream getUrl(String url) {
            logger.debug("Tentative de résolution de l'URL par CustomStreamFactory : {}", url);

            try {
                if (url.startsWith("file://")) {
                    Path filePath = Paths.get(new URI(url));
                    logger.info("Chemin de fichier résolu pour l'image (via CustomStreamFactory) : {}", filePath.toString());

                    if (Files.exists(filePath)) {
                        byte[] imageBytes = Files.readAllBytes(filePath);
                        return new ByteArrayFSStream(imageBytes);
                    } else {
                        logger.warn("Fichier image non trouvé par CustomStreamFactory à l'emplacement : {}", filePath.toString());
                    }
                } else {
                    logger.warn("URL non gérée par CustomStreamFactory (non 'file://') : {}", url);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la résolution de l'URL {} par CustomStreamFactory : {}", url, e.getMessage(), e);
            }
            return null;
        }
    }

    private static class ByteArrayFSStream implements com.openhtmltopdf.extend.FSStream {
        private final byte[] data;

        public ByteArrayFSStream(byte[] data) {
            this.data = data;
        }

        @Override
        public java.io.InputStream getStream() {
            return new java.io.ByteArrayInputStream(data);
        }

        @Override
        public java.io.Reader getReader() {
            return new java.io.InputStreamReader(getStream());
        }
    }
}