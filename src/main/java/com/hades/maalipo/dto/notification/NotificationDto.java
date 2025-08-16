package com.hades.maalipo.dto.notification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String titre;
    private String message;
    private String type;
    private Boolean lu;
    private LocalDateTime dateCreation;
    private Long referenceId;
    private String referenceType;

    // Informations supplémentaires pour l'affichage
    private String avatar; // URL de l'avatar de l'expéditeur
    private String lienAction; // URL pour l'action (ex: voir la demande)
    private String priorite; // HAUTE, NORMALE, BASSE
}