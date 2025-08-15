package com.hades.maalipo.service;

import com.hades.maalipo.dto.authen.AuditLogDto;
import com.hades.maalipo.model.AuditLog;
import com.hades.maalipo.repository.AuditLogRepository;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.specification.AuditLogSpecification;
import com.hades.maalipo.model.User; // À adapter selon ton modèle
import com.hades.maalipo.repository.UserRepository; // À adapter selon ton modèle
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
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        String role = user != null ? user.getRole().name() : null;

        Specification<AuditLog> spec = Specification.where(null);

        if ("ADMIN".equals(role)) {
            // ADMIN voit TOUT (y compris les actions de création)
            spec = Specification.where(AuditLogSpecification.hasUsername(username))
                    .and(AuditLogSpecification.hasEntityName(entityName))
                    .and(AuditLogSpecification.hasEntityId(entityId))
                    .and(AuditLogSpecification.hasAction(action));

        } else if ("EMPLOYEUR".equals(role)) {
            //  EMPLOYEUR voit son entreprise MAIS PAS les actions de création
            Long entrepriseId = (user.getEntreprise() != null) ? user.getEntreprise().getId() : null;
            List<Long> employeIds = employeRepository.findIdsByEntrepriseId(entrepriseId);

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
                    .and(AuditLogSpecification.excludeSystemAdminActions()) // ⚠️ AJOUTER cette ligne
                    .and(AuditLogSpecification.hasAction(action));

        } else if ("EMPLOYE".equals(role)) {
            // EMPLOYE voit ses actions MAIS PAS les actions de création
            spec = AuditLogSpecification.hasUsername(currentUsername)
                    .and(AuditLogSpecification.excludeSystemAdminActions()) // ⚠️ AJOUTER cette ligne
                    .and(AuditLogSpecification.hasAction(action));
        } else {
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


    public List<AuditLogDto> getAuditLogsByDateRange(LocalDateTime from, LocalDateTime to) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.between(root.get("dateAction"), from, to);
        List<AuditLog> logs = auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateAction"));
        return logs.stream().map(this::toDto).toList();
    }


}