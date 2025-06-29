// src/main/java/com/hades/paie1/enum1/MethodePaiement.java

package com.hades.paie1.enum1;

public enum MethodePaiement {
    VIREMENT("Virement bancaire"),
    CHEQUE("Chèque"),
    ESPECES("Espèces"),

    OM("ORANGE MONEY"),
    MOBILE_MONEY("Mobile Money"),
    AUTRE("Autre");

    private final String displayValue;

    MethodePaiement(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}