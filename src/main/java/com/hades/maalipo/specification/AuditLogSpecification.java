package com.hades.maalipo.specification;

import com.hades.maalipo.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public class AuditLogSpecification {
    public static Specification<AuditLog> hasUsername(String username) {
        return (root, query, cb) ->
                username == null ? null : cb.equal(root.get("username"), username);
    }

    public static Specification<AuditLog> hasEntityName(String entityName) {
        return (root, query, cb) ->
                entityName == null ? null : cb.equal(root.get("entityName"), entityName);
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, cb) ->
                entityId == null ? null : cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) ->
                action == null ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> excludeAdminActions() {
        return (root, query, cb) -> cb.and(
                cb.notEqual(root.get("action"), "CREATE_ENTREPRISE"),
                cb.notEqual(root.get("action"), "CREATE_EMPLOYEUR_USER"),
                cb.notEqual(root.get("action"), "CREATE_EMPLOYE_USER")
        );
    }

    public static Specification<AuditLog> excludeSystemAdminActions() {
        return (root, query, cb) -> cb.and(
                cb.notEqual(root.get("action"), "CREATE_ENTREPRISE"),
                cb.notEqual(root.get("action"), "CREATE_EMPLOYEUR_USER")
        );
    }

}
