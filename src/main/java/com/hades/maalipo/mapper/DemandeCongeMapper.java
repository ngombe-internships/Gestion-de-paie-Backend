package com.hades.maalipo.mapper;

import com.hades.maalipo.dto.conge.DemandeCongeCreateDto;
import com.hades.maalipo.dto.conge.DemandeCongeResponseDto;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;

public class DemandeCongeMapper {

    // Convertit un DTO de création en entité (pour la soumission)
    public static DemandeConge toEntity(DemandeCongeCreateDto dto, Employe employe) {
        if (dto == null) {
            return null;
        }

        return DemandeConge.builder()
                .employe(employe)
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .typeConge(dto.getTypeConge())
                .raison(dto.getRaison())
                .build();
    }

    // Version améliorée de la conversion en DTO de réponse
    public static DemandeCongeResponseDto toResponseDto(DemandeConge entity) {
        if (entity == null) {
            return null;
        }

        DemandeCongeResponseDto dto = new DemandeCongeResponseDto();
        dto.setId(entity.getId());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setTypeConge(entity.getTypeConge());
        dto.setRaison(entity.getRaison());
        dto.setStatut(entity.getStatut());
        dto.setDateDemande(entity.getDateDemande());
        dto.setDateApprobationRejet(entity.getDateApprobationRejet());
        dto.setMotifRejet(entity.getMotifRejet());

        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setEmployeNom(entity.getEmploye().getNom());
            dto.setEmployePrenom(entity.getEmploye().getPrenom());

            // Étape 2 : Récupérer le solde depuis l'entité Employe
            // Votre entité Employe devra avoir un champ pour le solde (Ex: getSoldeCongesPayes)
            // Pour le moment, nous utilisons un placeholder
            // dto.setSoldeConge(entity.getEmploye().getSoldeCongesPayes());

            // Mettre en place un TODO pour la future Phase 3
            dto.setSoldeConge(null);
        }

        return dto;
    }
}