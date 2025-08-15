package com.hades.maalipo.dto.conge;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HistoriqueCongeDto {
    private Long demandeId;
    private String typeConge;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal joursOuvrablesPris;
    private LocalDate dateApprobation;
    private String statut;
    private String raisonRejet;

    // Constructeurs
    public HistoriqueCongeDto() {}

    public HistoriqueCongeDto(Long demandeId, String typeConge, LocalDate dateDebut,
                              LocalDate dateFin, BigDecimal joursOuvrablesPris,
                              LocalDate dateApprobation, String statut, String raisonRejet) {
        this.demandeId = demandeId;
        this.typeConge = typeConge;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.joursOuvrablesPris = joursOuvrablesPris;
        this.dateApprobation = dateApprobation;
        this.statut = statut;
        this.raisonRejet = raisonRejet;
    }

    // Getters et Setters
    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getTypeConge() { return typeConge; }
    public void setTypeConge(String typeConge) { this.typeConge = typeConge; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getJoursOuvrablesPris() { return joursOuvrablesPris; }
    public void setJoursOuvrablesPris(BigDecimal joursOuvrablesPris) { this.joursOuvrablesPris = joursOuvrablesPris; }

    public LocalDate getDateApprobation() { return dateApprobation; }
    public void setDateApprobation(LocalDate dateApprobation) { this.dateApprobation = dateApprobation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getRaisonRejet() { return raisonRejet; }
    public void setRaisonRejet(String raisonRejet) { this.raisonRejet = raisonRejet; }
}