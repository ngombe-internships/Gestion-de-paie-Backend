package com.hades.maalipo.enum1;

public enum TypeConge {
    CONGE_PAYE("Congé payé annuel",
            18,
            true,
            false,
            15,
            "Congé annuel rémunéré selon ancienneté"),

    // ========== CONGÉS FAMILIAUX ==========
    CONGE_MATERNITE("Congé maternité", 98, false, true, 0, "14 semaines : 4 avant + 10 après accouchement"),
    CONGE_PATERNITE("Congé paternité", 2, false, true, 0, "2 jours ouvrables à la naissance"),
    CONGE_MARIAGE("Congé mariage", 4, false, true, 0, "4 jours ouvrables, une seule fois, justificatifs requis"),
    CONGE_DEUIL("Congé de deuil", 2, false, true, 0, "2 jours pour famille proche (conjoint, parent, enfant)"),

    // ========== CONGÉS MÉDICAUX ==========
    CONGE_MALADIE("Congé maladie", 365, false, true, 0, "Selon ancienneté, certificat médical obligatoire"),
    CONGE_ACCIDENT_TRAVAIL("Congé accident de travail", 365, false, true, 0, "Accident survenu dans le cadre professionnel"),

    // ========== AUTRES CONGÉS ==========
    CONGE_FORMATION("Congé formation", 30, false, false, 30, "Formation professionnelle avec accord employeur"),
    CONGE_SANS_SOLDE("Congé sans solde", 365, false, false, 30, "Congé exceptionnel non rémunéré"),
    CONGE_EXCEPTIONNEL("Congé exceptionnel", 5, false, true, 0, "Événements familiaux spéciaux");

    private final String libelle;
    private final int dureeMaximaleJours;
    private final boolean decompteDesCongesPayes;  // Décompte du solde congés payés
    private final boolean accordAutomatic;         // Accord automatique ou validation requise
    private final int delaiPreavisJours;          // Délai de préavis minimum
    private final String description;

    TypeConge(String libelle, int dureeMaximaleJours, boolean decompteDesCongesPayes,
              boolean accordAutomatic, int delaiPreavisJours, String description) {
        this.libelle = libelle;
        this.dureeMaximaleJours = dureeMaximaleJours;
        this.decompteDesCongesPayes = decompteDesCongesPayes;
        this.accordAutomatic = accordAutomatic;
        this.delaiPreavisJours = delaiPreavisJours;
        this.description = description;
    }

    // Getters
    public String getLibelle() { return libelle; }
    public int getDureeMaximaleJours() { return dureeMaximaleJours; }
    public boolean isDecompteDesCongesPayes() { return decompteDesCongesPayes; }
    public boolean isAccordAutomatic() { return accordAutomatic; }
    public int getDelaiPreavisJours() { return delaiPreavisJours; }
    public String getDescription() { return description; }

    public boolean requiresJustification() {
        return this == CONGE_MALADIE || this == CONGE_ACCIDENT_TRAVAIL ||
                this == CONGE_DEUIL || this == CONGE_MARIAGE;
    }

    public boolean isCongeFamilial() {
        return this == CONGE_MATERNITE || this == CONGE_PATERNITE ||
                this == CONGE_MARIAGE || this == CONGE_DEUIL;
    }

}
