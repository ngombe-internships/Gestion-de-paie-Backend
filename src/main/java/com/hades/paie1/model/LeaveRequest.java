package com.hades.paie1.model;

import com.hades.paie1.enum1.StatutConge;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="demandes_conge")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe; // Réfère à l'entité Employe

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Column(name = "date_debut_souhaitee", nullable = false)
    private LocalDate dateDebutSouhaitee;

    @Column(name = "date_fin_souhaitee", nullable = false)
    private LocalDate dateFinSouhaitee;

    @Column(name = "date_retour_effectif")
    private LocalDate dateRetourEffectif;

    @Column(name = "raison")
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutConge statut;

    @Column(name = "type_conge", nullable = false)
    private String typeConge;

    @Column(name = "date_approbation")
    private LocalDate dateApprobation;

    @Column(name = "raison_rejet")
    private String raisonRejet;

    @Column(name = "nombre_jours_valides")
    private Integer nombreJoursValides;


    //  Calcule le nombre de jours de congé entre la date de début et la date de fin, inclus.
    // @return Le nombre de jours de congé.
    @Transient
    public long getNombreJoursSouhaites() {
        if (this.dateDebutSouhaitee != null && this.dateFinSouhaitee != null) {
            return ChronoUnit.DAYS.between(this.dateDebutSouhaitee, this.dateFinSouhaitee) + 1;
        }
        return 0;
    }
}
