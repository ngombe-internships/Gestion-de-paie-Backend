package com.hades.maalipo.service.pdf;

import com.hades.maalipo.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UnifiedFileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedFileStorageService.class);

    @Autowired(required = false)
    private FileStorageService localFileStorageService;

    @Autowired(required = false)
    private CloudinaryService cloudinaryService;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public String saveFile(MultipartFile file) throws IOException {
        logger.info("Sauvegarde du fichier avec le profil actif : {}", activeProfile);

        if ("prod".equals(activeProfile)) {
            if (cloudinaryService == null) {
                throw new IOException("CloudinaryService non disponible en production");
            }
            logger.info("Utilisation de Cloudinary pour l'upload");
            return cloudinaryService.uploadFile(file);
        } else {
            if (localFileStorageService == null) {
                throw new IOException("FileStorageService non disponible en développement");
            }
            logger.info("Utilisation du stockage local pour l'upload");
            return localFileStorageService.saveFile(file);
        }
    }

    public void deleteFile(String fileUrl) {
        if ("prod".equals(activeProfile) && cloudinaryService != null) {
            cloudinaryService.deleteFile(fileUrl);
        }
    }
}
