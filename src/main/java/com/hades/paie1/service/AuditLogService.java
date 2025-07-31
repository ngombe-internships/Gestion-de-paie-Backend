package com.hades.paie1.service;

import com.hades.paie1.dto.AuditLogDto;
import com.hades.paie1.model.AuditLog;
import com.hades.paie1.repository.AuditLogRepository;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.utils.AuditLogSpecification;
import com.hades.paie1.model.User; // À adapter selon ton modèle
import com.hades.paie1.repository.UserRepository; // À adapter selon ton modèle
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository; // Il faut injecter ton UserRepository

    @Autowired
    private EmployeRepository employeRepository;
    public void logAction(String action, String entityName, Long entityId, String username, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .username(username)
                .dateAction(LocalDateTime.now())
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    public Page<AuditLogDto> getAuditLogs(String username, String entityName, Long entityId, String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateAction"));

        String currentUsername = getCurrentUsername();

        // Récupère le user courant pour savoir son rôle/entreprise
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        String role = user != null ? user.getRole().name() : null;

        Specification<AuditLog> spec = Specification.where(null);

        if ("ADMIN".equals(role)) {
            // ADMIN voit tout, mais on applique tout de même les filtres utilisateur s'ils sont renseignés
            spec = Specification.where(AuditLogSpecification.hasUsername(username))
                    .and(AuditLogSpecification.hasEntityName(entityName))
                    .and(AuditLogSpecification.hasEntityId(entityId))
                    .and(AuditLogSpecification.hasAction(action));
        } else if ("EMPLOYEUR".equals(role)) {
            // EMPLOYEUR : logs liés à son entreprise (entityName = Entreprise et entityId = son entreprise)
            Long entrepriseId = (user.getEntreprise() != null) ? user.getEntreprise().getId() : null;
            List<Long> employeIds = employeRepository.findIdsByEntrepriseId(entrepriseId); // À implémenter

            Specification<AuditLog> entrepriseSpec =
                    AuditLogSpecification.hasEntityName("Entreprise")
                            .and(AuditLogSpecification.hasEntityId(entrepriseId));
            Specification<AuditLog> employeSpec =
                    AuditLogSpecification.hasEntityName("Employe")
                            .and((root, query, cb) -> root.get("entityId").in(employeIds));
            Specification<AuditLog> selfSpec =
                    AuditLogSpecification.hasUsername(currentUsername);

            spec = Specification.where(entrepriseSpec)
                    .or(employeSpec)
                    .or(selfSpec)
                    .and(AuditLogSpecification.hasAction(action));
        } else if ("EMPLOYE".equals(role)) {
            // EMPLOYE : uniquement ses propres logs
            spec = AuditLogSpecification.hasUsername(currentUsername)
                    .and(AuditLogSpecification.hasAction(action));
        } else {
            // Si non authentifié, rien
            return Page.empty();
        }

        return auditLogRepository.findAll(spec, pageable).map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .username(log.getUsername())
                .dateAction(log.getDateAction())
                .details(log.getDetails())
                .build();
    }
}