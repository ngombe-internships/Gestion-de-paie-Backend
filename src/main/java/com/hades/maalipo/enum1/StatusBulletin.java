package com.hades.maalipo.enum1;
public enum StatusBulletin {
    GÉNÉRÉ,   // Bulletin initialement créé
    VALIDÉ,   // Bulletin vérifié et approuvé
    ENVOYÉ,   // Bulletin envoyé à l'employé
    ARCHIVÉ,  // Bulletin archivé (après envoi ou validation)
    ANNULÉ    // Bulletin annulé (peut-être avant envoi)
}