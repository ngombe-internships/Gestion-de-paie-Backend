package com.hades.paie1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    @Value("${file.upload-dir}")
    private String uploadDir;



    public String saveFile (MultipartFile file) throws IOException {

        logger.info("Tentative de sauvegarde du fichier : {}", file.getOriginalFilename());
        logger.info("Répertoire de destination configuré : {}", uploadDir);

        // 1. Normaliser et résoudre le chemin absolu du répertoire d'upload
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // 2. Créer le répertoire s'il n'existe pas
        try {
            Files.createDirectories(uploadPath);
            logger.info("Répertoire d'upload vérifié/créé : {}", uploadPath);
        } catch (IOException e) {
            logger.error("ERREUR CRITIQUE : Impossible de créer le répertoire d'upload {}. Vérifiez les permissions du dossier et le chemin. Message : {}", uploadPath, e.getMessage(), e);
            // Relancez l'exception pour que le contrôleur sache que l'upload a échoué
            throw new IOException("Impossible de créer le répertoire d'upload : " + uploadPath, e);
        }

        // 3. Vérifier si le répertoire est accessible en écriture (utile pour le débogage)
        if (!Files.isWritable(uploadPath)) {
            logger.error("ERREUR CRITIQUE : Le répertoire d'upload n'est pas accessible en écriture : {}", uploadPath);
            throw new IOException("Le répertoire d'upload n'est pas accessible en écriture : " + uploadPath);
        }

        // 4. Générer un nom de fichier unique pour éviter les collisions
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        logger.info("Nom de fichier généré : {}", fileName);
        logger.info("Chemin complet du fichier cible : {}", filePath);

        // 5. Copier le fichier dans le répertoire cible
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Fichier sauvegardé avec succès à : {}", filePath);
        } catch (IOException e) {
            logger.error("ERREUR CRITIQUE : Impossible de copier le fichier {}. Message : {}", fileName, e.getMessage(), e);
            throw new IOException("Impossible de sauvegarder le fichier : " + fileName, e);
        }

        // 6. Retourner l'URL relative pour l'enregistrement en base de données
        // Assurez-vous que votre application web est configurée pour servir ce chemin '/logos/**'
        String publicUrl = "/logos/" + fileName;
        logger.info("URL publique du logo générée : {}", publicUrl);
        return publicUrl;
    }
}


