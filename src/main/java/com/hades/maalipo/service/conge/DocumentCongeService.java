package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.DocumentDto;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Document;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.DocumentRepository;
import com.hades.maalipo.service.AuditLogService;
import com.hades.maalipo.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentCongeService {

    @Value("${app.storage.documents:C:/uploads/documents}")
    private String documentStoragePath;


    private final  DemandeCongeRepository demandeCongeRepository;
    private final  DocumentRepository documentRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;


    public DocumentCongeService(
            DemandeCongeRepository demandeCongeRepository,
            DocumentRepository documentRepository,
            CloudinaryService cloudinaryService,
            AuditLogService auditLogService
    ){
        this.cloudinaryService =cloudinaryService;
        this.demandeCongeRepository = demandeCongeRepository;
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
    }


    private static final List<String> EXTENSIONS_AUTORISEES = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final long TAILLE_MAX_FICHIER = 5 * 1024 * 1024; // 5MB

    public enum TypeDocument {
        CERTIFICAT_MEDICAL("Certificat médical", Arrays.asList("pdf", "jpg", "png")),
        ACTE_NAISSANCE("Acte de naissance", Arrays.asList("pdf", "jpg", "png")),
        ACTE_MARIAGE("Acte de mariage", Arrays.asList("pdf", "jpg", "png")),
        ACTE_DECES("Acte de décès", Arrays.asList("pdf", "jpg", "png")),
        JUSTIFICATIF_FAMILLE("Justificatif lien familial", Arrays.asList("pdf", "jpg", "png"));

        private final String libelle;
        private final List<String> formatsAcceptes;

        TypeDocument(String libelle, List<String> formatsAcceptes) {
            this.libelle = libelle;
            this.formatsAcceptes = formatsAcceptes;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /**
     * Stocke un document pour une demande de congé
     */
    public String stockerDocument(MultipartFile file, Long demandeId, TypeDocument typeDocument, User currentUser)
            throws IOException {
        // Validation du fichier
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        if (file.getSize() > TAILLE_MAX_FICHIER) {
            throw new IllegalArgumentException("Taille de fichier dépassée (max 5MB)");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Type de fichier non autorisé");
        }

        // Récupération et validation de la demande
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Création du dossier pour cette demande
        Path folderPath = Paths.get(documentStoragePath).resolve(demandeId.toString());
        Files.createDirectories(folderPath);

        // Génération d'un nom unique pour le fichier
        String fileName = typeDocument.name() + "_" + UUID.randomUUID() + "." + extension;
        Path filePath = folderPath.resolve(fileName);

        // Enregistrement physique du fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Enregistrement dans la base de données
        Document document = new Document();
        document.setNom(StringUtils.cleanPath(file.getOriginalFilename()));
        document.setChemin(filePath.toString());
        document.setType(typeDocument.name());
        document.setContentType(file.getContentType());
        document.setTaille(file.getSize());
        document.setDateUpload(LocalDateTime.now());
        document.setDemandeConge(demande);
        document.setUploadedBy(currentUser);

        documentRepository.save(document);

        return filePath.toString();
    }

    // Code existant...

    /**
     * Récupère tous les documents d'une demande
     */
    public List<DocumentDto> getDocumentsForDemande(Long demandeId) {
        List<Document> documents = documentRepository.findByDemandeCongeId(demandeId);

        return documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private DocumentDto convertToDto(Document document) {
        DocumentDto dto = new DocumentDto();
        dto.setId(document.getId());
        dto.setNom(document.getNom());
        dto.setType(document.getType());
        dto.setContentType(document.getContentType());
        dto.setTaille(document.getTaille());
        dto.setDateUpload(document.getDateUpload());
        dto.setUrl(document.getUrl()); // Ajout de l'URL Cloudinary
        dto.setUploadedBy(document.getUploadedBy().getUsername());
        return dto;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // Méthode de validation existante
    public ValidationResult validerDocumentsRequis(TypeConge typeConge, List<String> documentsUpload) {
        List<String> erreurs = new ArrayList<>();

        switch (typeConge) {
            case CONGE_MALADIE:
            case CONGE_ACCIDENT_TRAVAIL:
                if (documentsUpload == null || !containsType(documentsUpload, "certificat_medical")) {
                    erreurs.add("Certificat médical obligatoire pour congé maladie");
                }
                break;

            // Autres cas...
        }

        return new ValidationResult(erreurs.isEmpty(), erreurs);
    }

    private boolean containsType(List<String> documents, String type) {
        return documents.stream().anyMatch(doc -> doc.toLowerCase().contains(type));
    }

    public static class ValidationResult {
        private final boolean valide;
        private final List<String> erreurs;

        public ValidationResult(boolean valide, List<String> erreurs) {
            this.valide = valide;
            this.erreurs = erreurs;
        }

        public boolean isValide() { return valide; }
        public List<String> getErreurs() { return erreurs; }
        public String getMessageErreurs() { return String.join("; ", erreurs); }
    }

    public DocumentDto uploadToCloudinary(MultipartFile file, Long demandeId, TypeDocument typeDocument, User user) throws IOException {
        // Upload vers Cloudinary
        Map uploadResult = cloudinaryService.uploadCongeDocument(file);
        String cloudinaryUrl = (String) uploadResult.get("url");
        String publicId = (String) uploadResult.get("public_id");

        // Créer le document en base
        Document document = new Document();
        document.setNom(file.getOriginalFilename());
        document.setUrl(cloudinaryUrl);
        document.setPublicId(publicId); // Ajoutez ce champ à votre entité Document
        document.setType(typeDocument.name());
        document.setContentType(file.getContentType());
        document.setTaille(file.getSize());
        document.setDateUpload(LocalDateTime.now());
        document.setDemandeConge(getDemandeConge(demandeId));
        document.setUploadedBy(user);

        document = documentRepository.save(document);

        auditLogService.logAction(
                "UPLOAD_DOCUMENT_CONGE",
                "Document",
                document.getId(),
                user.getUsername(),
                String.format("Document uploadé: %s (%s) pour la demande %d",
                        file.getOriginalFilename(),
                        typeDocument.getLibelle(),
                        demandeId)
        );

        return convertToDto(document);
    }

    // Méthode utilitaire pour récupérer la demande
    private DemandeConge getDemandeConge(Long demandeId) {
        return demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande congé non trouvée: " + demandeId));
    }


}