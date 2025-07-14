package com.hades.paie1.model;

import com.hades.paie1.enum1.StatutDemandeConge;
import com.hades.paie1.enum1.TypeConge;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name="demandes_conge")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "raison", length = 500)
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutDemandeConge statut; // Enum à créer

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    @Column(name = "date_approbation_rejet")
    private LocalDate dateApprobationRejet;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approuvee_par_user_id")
    private User approuveePar;
}
