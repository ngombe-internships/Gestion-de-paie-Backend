package com.hades.maalipo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;  // Nom original du fichier

    @Column(nullable = true)
    private String chemin;  // Chemin physique du fichier

    @Column(nullable = false)
    private String type;  // Type de document (CERTIFICAT_MEDICAL, ACTE_MARIAGE, etc.)

    @Column
    private String contentType;  // Type MIME (application/pdf, image/jpeg, etc.)

    @Column
    private Long taille;  // Taille en octets

    @Column
    private LocalDateTime dateUpload;  // Date d'upload


    @Column(columnDefinition = "TEXT")
    private String url;  // URL Cloudinary

    private String publicId;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_conge_id")
    private DemandeConge demandeConge;  // Demande associée

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;  // Utilisateur qui a uploadé
}