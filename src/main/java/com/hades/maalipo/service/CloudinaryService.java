package com.hades.maalipo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
//@Profile("prod")
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload un fichier pour les logos d'entreprise
     */
    public String uploadLogo(MultipartFile file) throws IOException {
        try {
            //log.info("Début de l'upload du logo vers Cloudinary : {}", file.getOriginalFilename());

            String publicId = UUID.randomUUID().toString() + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "");

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "company_logos",
                    "resource_type", "image",
                    "width", 300,
                    "height", 300,
                    "crop", "limit",
                    "quality", "auto:good"
            );

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );

            String imageUrl = uploadResult.get("secure_url").toString();
           // log.info("Upload logo réussi. URL: {}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
           // log.error("Erreur lors de l'upload du logo : {}", e.getMessage(), e);
            throw new IOException("Échec de l'upload vers Cloudinary", e);
        }
    }

    /**
     * Upload un document de congé
     */
    public Map<String, Object> uploadCongeDocument(MultipartFile file) {
        try {

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", "conge_documents",
                    "resource_type", "auto",
                    "public_id", generatePublicId(file)
            );

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return uploadResult;

        } catch (IOException e) {
           // log.error("Erreur lors de l'upload du document de congé", e);
            throw new RuntimeException("Erreur lors de l'upload du fichier vers Cloudinary", e);
        }
    }

    /**
     * Supprime un fichier de Cloudinary
     */
    public void deleteFile(String imageUrl) {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
              //  log.info("Fichier supprimé de Cloudinary : {}", publicId);
            }
        } catch (Exception e) {
           // log.error("Erreur lors de la suppression : {}", e.getMessage());
        }
    }

    private String generatePublicId(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "conge_doc_" + UUID.randomUUID().toString() + extension;
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl.contains("cloudinary.com")) {
                String[] parts = imageUrl.split("/");
                for (int i = 0; i < parts.length; i++) {
                    if ("upload".equals(parts[i]) && i + 2 < parts.length) {
                        StringBuilder publicId = new StringBuilder();
                        for (int j = i + 2; j < parts.length; j++) {
                            if (j > i + 2) publicId.append("/");
                            publicId.append(parts[j]);
                        }
                        String result = publicId.toString();
                        int lastDot = result.lastIndexOf('.');
                        return lastDot > 0 ? result.substring(0, lastDot) : result;
                    }
                }
            }
        } catch (Exception e) {
            //log.warn("Impossible d'extraire le public_id de l'URL : {}", imageUrl);
        }
        return null;
    }
}