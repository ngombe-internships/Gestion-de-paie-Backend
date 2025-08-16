package com.hades.maalipo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private TypeNotification type;

    @Column(name = "lu", nullable = false)
    private Boolean lu = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Référence vers l'objet concerné (optionnel)
    private Long referenceId;
    private String referenceType; // "DEMANDE_CONGE", "BULLETIN_PAIE", etc.

    public enum TypeNotification {
        DEMANDE_CONGE_SOUMISE,
        DEMANDE_CONGE_APPROUVEE,
        DEMANDE_CONGE_REJETEE,
        DEMANDE_CONGE_ANNULEE,
        RAPPEL_DEMANDE_ATTENTE,
        BULLETIN_PAIE_DISPONIBLE,
        INFORMATION_GENERALE
    }


}