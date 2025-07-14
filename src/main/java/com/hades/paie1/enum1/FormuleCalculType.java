package com.hades.paie1.enum1;

public enum FormuleCalculType {
    MONTANT_FIXE,
    NOMBRE_BASE_TAUX, // Ex: heures sup (nombre * taux horaire * taux majoration)
    POURCENTAGE_BASE, // Ex: cotisation (base * taux)
    BAREME, // Ex: IRPP (calcul selon tranches de revenus)
    AUTRE,
    TAUX_DEFAUT_X_MONTANT_DEFAUT,            // <--- Ajoute cette ligne !
    NOMBRE_X_TAUX_DEFAUT_X_MONTANT_DEFAUT
}
