package com.hades.paie1.controller;

import com.hades.paie1.dto.AuditLogDto;
import com.hades.paie1.model.AuditLog;
import com.hades.paie1.repository.AuditLogRepository;
import com.hades.paie1.service.AuditLogService;
import com.hades.paie1.utils.AuditLogSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public Page<AuditLogDto> getAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return auditLogService.getAuditLogs(username, entityName, entityId, action, page, size);
    }
}
