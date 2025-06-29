package com.hades.paie1.service;

import com.hades.paie1.dto.BulletinPaieEmployeurDto;
import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder; // N'oubliez pas cette importation
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
import java.net.URI; // Nouvelle importation requise pour Paths.get(new URI(url))

@Service
public class PdfService {

    @Value("${file.upload-dir}")
    private String uploadDir; // Cela sera C:/uploads/logos grâce à votre configuration

    private final TemplateEngine templateEngine;

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateBulletinPdf(BulletinPaieResponseDto bulletinData) throws IOException {
        try {
            // Logique pour construire l'URL complète du logo ---
            if (bulletinData.getEntreprise() != null && bulletinData.getEntreprise().getLogoUrl() != null && !bulletinData.getEntreprise().getLogoUrl().isEmpty()) {
                String relativeLogoPath = bulletinData.getEntreprise().getLogoUrl(); // Ex: "/logos/UUID_filename.png"

                String fileName;
                if(relativeLogoPath.contains("/logos")) {
                    fileName = relativeLogoPath.substring(relativeLogoPath.lastIndexOf("/") + 1);

                }else {
                    fileName = relativeLogoPath;
                }

                 Path absoluteLogoPath = Paths.get(uploadDir).resolve(fileName);

                String htmlLogoUrl = absoluteLogoPath.toUri().toString();
                logger.info("URL du logo générée pour le PDF (HTML) : {}", htmlLogoUrl);

                // Met à jour le DTO avec l'URL complète pour que Thymeleaf l'utilise
                bulletinData.getEntreprise().setLogoUrl(htmlLogoUrl);
            }


            Context context = new Context();
            context.setVariable("bulletin", bulletinData);

            String html = templateEngine.process("bulletin-template", context);

            // Parsing et nettoyage du HTML avec JSoup
            Document document = Jsoup.parse(html);
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            String cleanHtml = document.html(); // Utilise le HTML propre de Jsoup

            // Génération du PDF avec OpenHTMLToPDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(cleanHtml, null); // Le baseUri ici peut rester null car les URLs d'images sont absolues
            builder.toStream(outputStream);

            // Configuration du répertoire de base pour les ressources (CSS si relatifs, etc.)
            // Note: Pour les images avec file://, la CustomStreamFactory sera utilisée.
            String baseUriForRelativeResources = Paths.get(uploadDir).getParent().toUri().toString();
            builder.withUri(baseUriForRelativeResources); // Ex: file:///C:/uploads

            // Configuration pour la gestion des images via le protocole "file"
            // La CustomStreamFactory gérera toutes les URLs qui commencent par "file://"
            builder.useProtocolsStreamImplementation(new CustomStreamFactory(), "file");

            logger.info("Base URI configurée pour le rendu PDF (ressources relatives) : {}", baseUriForRelativeResources);

            builder.run();

            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF : {}", e.getMessage(), e);
            throw new IOException("Erreur lors de la génération du PDF", e);
        }
    }

    // Classe pour gérer les ressources personnalisées (images)
    // Elle est maintenant statique et ne dépend plus de 'uploadDir' du service,
    // car elle lira les chemins 'file://' directement.
    private static class CustomStreamFactory implements com.openhtmltopdf.extend.FSStreamFactory {

        private static final Logger logger = LoggerFactory.getLogger(CustomStreamFactory.class);

        // Pas besoin de constructeur prenant uploadDir ici, car on lit directement l'URI file://

        @Override
        public com.openhtmltopdf.extend.FSStream getUrl(String url) {
            logger.debug("Tentative de résolution de l'URL par CustomStreamFactory : {}", url);

            try {
                // S'assure que c'est bien une URL de fichier
                if (url.startsWith("file://")) {
                    // Convertit l'URL file:// en un objet Path Java pour le système de fichiers
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
            return null; // Retourne null si l'URL n'est pas un fichier ou si une erreur se produit
        }
    }

    // Classe pour encapsuler les bytes d'une image
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



    public String generateHtmlContentForPdf (BulletinPaieResponseDto bulletinData) {

        BulletinPaieResponseDto dataForHtml = cloneBulletinData(bulletinData);

        if (dataForHtml.getEntreprise() != null && dataForHtml.getEntreprise().getLogoUrl() != null && !dataForHtml.getEntreprise().getLogoUrl().isEmpty()) {
            String currentLogoUrl = dataForHtml.getEntreprise().getLogoUrl();

            String finalLogoUrl;
            if (currentLogoUrl.startsWith("file://")) {
                String fileName = currentLogoUrl.substring(currentLogoUrl.lastIndexOf("/") + 1);
                finalLogoUrl = "http://localhost:8081/logos/" + fileName; // <-- CHANGEMENT ICI
            } else if (!currentLogoUrl.startsWith("http://") && !currentLogoUrl.startsWith("https://")) {
                // Si c'est juste un nom de fichier ou un chemin relatif (ex: /logos/...)
                // ASSUREZ-VOUS QUE LE CHEMIN /logos/ EST BIEN ABSOLU OU RELATIF À LA RACINE ET PRÉFIXEZ-LE.
                // Si c'est déjà /logos/UUID_filename.jpg, il suffit d'ajouter le domaine.
                if (currentLogoUrl.startsWith("/")) {
                    finalLogoUrl = "http://localhost:8081" + currentLogoUrl; // <-- CHANGEMENT ICI
                } else {
                    finalLogoUrl = "http://localhost:8081/logos/" + currentLogoUrl; // <-- CHANGEMENT ICI
                }
            } else {
                finalLogoUrl = currentLogoUrl; // C'est déjà une URL complète HTTP(S)
            }
            dataForHtml.getEntreprise().setLogoUrl(finalLogoUrl);
            logger.info("URL du logo générée pour le HTML de prévisualisation (context Web via Angular) : {}", dataForHtml.getEntreprise().getLogoUrl());
        }

        Context context = new Context();
        context.setVariable("bulletin", dataForHtml);
        context.setVariable("isPreview", true);

     return templateEngine.process("bulletin-template", context);
    }


private BulletinPaieResponseDto cloneBulletinData(BulletinPaieResponseDto original) {
        BulletinPaieResponseDto clone = new BulletinPaieResponseDto();

        // Copier tous les champs primitifs et String
        clone.setId(original.getId());
        clone.setTauxHoraire(original.getTauxHoraire());
        clone.setHeuresNormal(original.getHeuresNormal());
        clone.setSalaireBase(original.getSalaireBase());
        clone.setHeureSup1(original.getHeureSup1());
        clone.setHeureSup2(original.getHeureSup2());
        clone.setHeureNuit(original.getHeureNuit());
        clone.setHeureFerie(original.getHeureFerie());
        clone.setPrimeTransport(original.getPrimeTransport());
        clone.setPrimePonctualite(original.getPrimePonctualite());
        clone.setPrimeAnciennete(original.getPrimeAnciennete());
        clone.setPrimeRendement(original.getPrimeRendement());
        clone.setPrimeTechnicite(original.getPrimeTechnicite());
        clone.setTotalPrimes(original.getTotalPrimes());
        clone.setSalaireBrut(original.getSalaireBrut());
        clone.setSalaireImposable(original.getSalaireImposable());
        clone.setBaseCnps(original.getBaseCnps());
        clone.setIrpp(original.getIrpp());
        clone.setCac(original.getCac());
        clone.setTaxeCommunale(original.getTaxeCommunale());
        clone.setRedevanceAudioVisuelle(original.getRedevanceAudioVisuelle());
        clone.setCnpsVieillesseSalarie(original.getCnpsVieillesseSalarie());
        clone.setCreditFoncierSalarie(original.getCreditFoncierSalarie());
        clone.setFneSalarie(original.getFneSalarie());
        clone.setTotalRetenues(original.getTotalRetenues());
        clone.setCnpsVieillesseEmployeur(original.getCnpsVieillesseEmployeur());
        clone.setCnpsAllocationsFamiliales(original.getCnpsAllocationsFamiliales());
        clone.setCnpsAccidentsTravail(original.getCnpsAccidentsTravail());
        clone.setCreditFoncierPatronal(original.getCreditFoncierPatronal());
        clone.setFnePatronal(original.getFnePatronal());
        clone.setTotalChargesPatronales(original.getTotalChargesPatronales());
        clone.setSalaireNet(original.getSalaireNet());
        clone.setCoutTotalEmployeur(original.getCoutTotalEmployeur());
        clone.setCotisationCnps(original.getCotisationCnps());
        clone.setPeriodePaie(original.getPeriodePaie());
        clone.setDateCreationBulletin(original.getDateCreationBulletin());
        clone.setStatusBulletin(original.getStatusBulletin());
        clone.setDatePaiement(original.getDatePaiement());
        clone.setMethodePaiement(original.getMethodePaiement());
        // Cloner les objets imbriqués
        // Pour EmployeDto, une simple copie de référence peut suffire si l'objet n'est pas modifié.
        if (original.getEmploye() != null) {
            clone.setEmploye(original.getEmploye());
        }

        // Pour EntrepriseDto, nous allons modifier son 'logoUrl', donc il faut le cloner
        // pour ne pas modifier l'objet original.
        if (original.getEntreprise() != null) {
            EntrepriseDto clonedEntreprise = new EntrepriseDto();
            clonedEntreprise.setId(original.getEntreprise().getId());
            clonedEntreprise.setNom(original.getEntreprise().getNom());
            clonedEntreprise.setNumeroSiret(original.getEntreprise().getNumeroSiret());
            clonedEntreprise.setAdresseEntreprise(original.getEntreprise().getAdresseEntreprise());
            clonedEntreprise.setTelephoneEntreprise(original.getEntreprise().getTelephoneEntreprise());
            clonedEntreprise.setEmailEntreprise(original.getEntreprise().getEmailEntreprise());
            clonedEntreprise.setLogoUrl(original.getEntreprise().getLogoUrl());
            // Ajoutez ici d'autres champs si EntrepriseDto en a plus
            clone.setEntreprise(clonedEntreprise);
        }

        return clone;
    }
}