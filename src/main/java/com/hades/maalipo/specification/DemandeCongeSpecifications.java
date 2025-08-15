package com.hades.maalipo.specification;

import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.model.DemandeConge;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class DemandeCongeSpecifications {

    public static Specification<DemandeConge> withEmployeId(Long employeId) {
        return (root, query, cb) -> cb.equal(root.get("employe").get("id"), employeId);
    }

    public static Specification<DemandeConge> withStatut(String statut) {
        return (root, query, cb) -> statut == null || statut.equals("TOUS") ? null :
                cb.equal(root.get("statut"), StatutDemandeConge.valueOf(statut));
    }

    public static Specification<DemandeConge> withYear(Integer year) {
        return (root, query, cb) -> year == null ? null :
                cb.equal(cb.function("YEAR", Integer.class, root.get("dateDebut")), year);
    }

    public static Specification<DemandeConge> withSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isEmpty()) {
                return null;
            }
            String searchTermLower = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("raison")), searchTermLower),
                    cb.like(cb.lower(root.get("typeConge").as(String.class)), searchTermLower)
            );
        };
    }
}