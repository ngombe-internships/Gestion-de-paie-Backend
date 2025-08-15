package com.hades.maalipo.dto.conge;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hades.maalipo.enum1.TypeConge;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TypeCongeConfigResponseDto {
    private Long id;
    private TypeConge typeConge;
    private String typeCongeLibelle;
    private Integer dureeMaximaleJours;
    private Integer delaiPreavisJours;
    private BigDecimal pourcentageRemuneration;
    private String documentsRequis;
    private String conditionsAttribution;
    private Boolean cumulAutorise;
    private Boolean actif;
    private Boolean documentsObligatoires;
    private Long entrepriseId;
    private String entrepriseNom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateMiseAJour;

    // Constructeurs
    public TypeCongeConfigResponseDto() {
        this.dateMiseAJour = LocalDateTime.now();
    }

    // Getters et Setters complets
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TypeConge getTypeConge() { return typeConge; }
    public void setTypeConge(TypeConge typeConge) {
        this.typeConge = typeConge;
        this.typeCongeLibelle = typeConge != null ? typeConge.getLibelle() : null;
    }

    public String getTypeCongeLibelle() { return typeCongeLibelle; }
    public void setTypeCongeLibelle(String typeCongeLibelle) { this.typeCongeLibelle = typeCongeLibelle; }

    public Integer getDureeMaximaleJours() { return dureeMaximaleJours; }
    public void setDureeMaximaleJours(Integer dureeMaximaleJours) { this.dureeMaximaleJours = dureeMaximaleJours; }

    public Integer getDelaiPreavisJours() { return delaiPreavisJours; }
    public void setDelaiPreavisJours(Integer delaiPreavisJours) { this.delaiPreavisJours = delaiPreavisJours; }

    public BigDecimal getPourcentageRemuneration() { return pourcentageRemuneration; }
    public void setPourcentageRemuneration(BigDecimal pourcentageRemuneration) { this.pourcentageRemuneration = pourcentageRemuneration; }

    public String getDocumentsRequis() { return documentsRequis; }
    public void setDocumentsRequis(String documentsRequis) { this.documentsRequis = documentsRequis; }

    public String getConditionsAttribution() { return conditionsAttribution; }
    public void setConditionsAttribution(String conditionsAttribution) { this.conditionsAttribution = conditionsAttribution; }

    public Boolean getCumulAutorise() { return cumulAutorise; }
    public void setCumulAutorise(Boolean cumulAutorise) { this.cumulAutorise = cumulAutorise; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Boolean getDocumentsObligatoires() { return documentsObligatoires; }
    public void setDocumentsObligatoires(Boolean documentsObligatoires) { this.documentsObligatoires = documentsObligatoires; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
}