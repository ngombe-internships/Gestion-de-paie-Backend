package com.hades.paie1.dto;

import com.hades.paie1.enum1.StatutDemandeConge;
import com.hades.paie1.enum1.TypeConge;
import com.hades.paie1.model.DemandeConge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor

public class DemandeCongeResponseDto {

    private Long id;
    private Long employeId;
    private String employeNom;
    private String employePrenom;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private TypeConge typeConge;
    private String raison;
    private StatutDemandeConge statut;
    private LocalDate dateDemande;
    private LocalDate dateApprobationRejet;
    private String motifRejet;
    private BigDecimal soldeConge;


    public DemandeCongeResponseDto(DemandeConge demandeConge) {
        this.id = demandeConge.getId();
        this.dateDebut = demandeConge.getDateDebut();
        this.dateFin = demandeConge.getDateFin();
        this.typeConge = demandeConge.getTypeConge();
        this.raison = demandeConge.getRaison();
        this.statut = demandeConge.getStatut();
        this.dateDemande = demandeConge.getDateDemande();
        this.dateApprobationRejet = demandeConge.getDateApprobationRejet();
        this.motifRejet = demandeConge.getMotifRejet();

        // Informations sur l'employé
        if (demandeConge.getEmploye() != null) {
            this.employeId = demandeConge.getEmploye().getId();
            this.employeNom = demandeConge.getEmploye().getNom();
            this.employePrenom = demandeConge.getEmploye().getPrenom();
            this.soldeConge = demandeConge.getEmploye().getSoldeJoursConge();
        }

    }

}
