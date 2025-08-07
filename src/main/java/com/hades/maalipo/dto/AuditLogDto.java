package com.hades.maalipo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    private Long id;
    private String action;
    private String entityName;
    private Long entityId;
    private String username;
    private LocalDateTime dateAction;
    private String details;
}
