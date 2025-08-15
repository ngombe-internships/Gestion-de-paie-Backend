package com.hades.maalipo.dto.conge;

import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.model.DemandeConge;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AbsenceJourneeDto {

    private Long demandeId;
    private Long employeId;
    private String employePrenom;
    private String employeNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateSpecifique; // Date spécifique pour l'affichage calendrier
    private TypeConge typeConge;
    private StatutDemandeConge statut;
    private String couleurStatut;
    private boolean sousEffectif; // Indicateur de sous-effectif pour ce jour
    private double tauxAbsenteisme; // Pourcentage d'employés absents ce jour

    // Constructeur pour mapper facilement une entité DemandeConge
    public AbsenceJourneeDto(DemandeConge demande) {
        this.demandeId = demande.getId();
        this.dateDebut = demande.getDateDebut();
        this.dateFin = demande.getDateFin();
        this.typeConge = demande.getTypeConge();
        this.statut = demande.getStatut();

        // Mapping de l'employé
        if (demande.getEmploye() != null) {
            this.employeId = demande.getEmploye().getId();
            this.employePrenom = demande.getEmploye().getPrenom();
            this.employeNom = demande.getEmploye().getNom();
        }

        // Logique pour déterminer une couleur d'affichage
        this.couleurStatut = this.mapStatutToCouleur(demande.getStatut());
    }

    // Méthode utilitaire pour le mapping de la couleur
    private String mapStatutToCouleur(StatutDemandeConge statut) {
        if (statut == null) {
            return "gris"; // Statut non défini
        }
        switch (statut) {
            case EN_ATTENTE:
                return "orange";
            case APPROUVEE:
                return "vert";
            case REJETEE:
                return "rouge";
            default:
                return "gris";
        }
    }
}