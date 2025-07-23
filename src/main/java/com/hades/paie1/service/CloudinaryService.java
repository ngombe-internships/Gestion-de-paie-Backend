package com.hades.paie1.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("prod")
public class CloudinaryService {
    private static  final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        try {
            logger.info("Debut de l'upload vers Cloudinary pour le fichier : {}", file.getOriginalFilename());

            // Generer un nom unique (sans le préfixe "logos/" car on utilise folder)
            String publicId = UUID.randomUUID().toString() + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "");

            // Configuration de l'upload - SOLUTION 1 : Paramètres directs
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "company_logos",
                    "resource_type", "image",
                    "width", 300,
                    "height", 300,
                    "crop", "limit",
                    "quality", "auto:good",
                    //"format", "auto"
            );

            // Upload vers Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );

            String imageUrl = uploadResult.get("secure_url").toString();
            logger.info("Upload Cloudinary réussi. URL: {}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
            logger.error("Erreur lors de l'upload Cloudinary : {}", e.getMessage(), e);
            throw new IOException("Echec de l'upload vers Cloudinary", e);
        }
    }
    public void deleteFile(String imageUrl) {
          try {
              String publicId = extractPublicIdFromUrl(imageUrl);
              if(publicId != null){
                  cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                  logger.info("fichier supprimé de Cloudinary : {}",publicId);
              }
          } catch (Exception e){
              logger.error("Erreur lors de la suppression Cloudinary : {}", e.getMessage());
          }
    }
    private String extractPublicIdFromUrl(String imageUrl) {
        // Exemple d'URL : https://res.cloudinary.com/your-cloud/image/upload/v1234567890/company_logos/logos/uuid_filename.jpg
        try {
            if (imageUrl.contains("cloudinary.com")) {
                String[] parts = imageUrl.split("/");
                for (int i = 0; i < parts.length; i++) {
                    if ("upload".equals(parts[i]) && i + 2 < parts.length) {
                        // Le public_id commence après "upload/v{version}/"
                        StringBuilder publicId = new StringBuilder();
                        for (int j = i + 2; j < parts.length; j++) {
                            if (j > i + 2) publicId.append("/");
                            publicId.append(parts[j]);
                        }
                        // Supprimer l'extension
                        String result = publicId.toString();
                        int lastDot = result.lastIndexOf('.');
                        return lastDot > 0 ? result.substring(0, lastDot) : result;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible d'extraire le public_id de l'URL : {}", imageUrl);
        }
        return null;
    }





}
