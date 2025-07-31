package com.hades.paie1.utils;

import com.hades.paie1.model.AuditLog;
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
}
