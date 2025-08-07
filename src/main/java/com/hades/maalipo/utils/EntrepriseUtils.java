package com.hades.maalipo.utils;

import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.Entreprise;

public class EntrepriseUtils {
    public static Long resolveEntrepriseId(Object obj) {
        if (obj instanceof BulletinPaie bp && bp.getEntreprise() != null) {
            return bp.getEntreprise().getId();
        } else if (obj instanceof Employe emp && emp.getEntreprise() != null) {
            return emp.getEntreprise().getId();
        } else if (obj instanceof Entreprise ent) {
            return ent.getId();
        }
        return null;
    }
}
