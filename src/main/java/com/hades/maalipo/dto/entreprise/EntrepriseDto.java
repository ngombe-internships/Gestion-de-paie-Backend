package com.hades.maalipo.dto.entreprise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseDto {
    private Long id;
    private String nom;
    private String adresseEntreprise;
    private String emailEntreprise;
    private String telephoneEntreprise;
    private String numeroSiret;
    private String logoUrl;
    private LocalDate dateCreation;

    private Long employeurPrincipalId;
    private String employeurPrincipalUsername;
    private LocalDateTime dateCreationSysteme;
    private LocalDateTime dateDerniereMiseAJour;

    //nouveau
//    private Double latitudeEntreprise;
//    private Double longitudeEntreprise;
//    private Integer radiusToleranceMeters;
    private BigDecimal standardHeuresHebdomadaires;
    private Integer standardJoursOuvrablesHebdomadaires;

    private boolean active;
    private int nombreEmployes;

}
