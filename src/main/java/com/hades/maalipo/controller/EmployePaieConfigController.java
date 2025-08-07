package com.hades.maalipo.controller;

import com.hades.maalipo.dto.EmployePaieConfigDto;
import com.hades.maalipo.model.EmployePaieConfig;
import com.hades.maalipo.service.EmployePaieConfigService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employe-paie-config")
public class EmployePaieConfigController {

    private final EmployePaieConfigService employePaieConfigService;

    public EmployePaieConfigController(EmployePaieConfigService employePaieConfigService) {
        this.employePaieConfigService = employePaieConfigService;
    }

    @GetMapping
    public ResponseEntity<List<EmployePaieConfigDto>> getAllEmployePaieConfigs() {
        List<EmployePaieConfigDto> configs = employePaieConfigService.getAllEmployePaieConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/my-company")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<List<EmployePaieConfigDto>> getMyCompanyEmployePaieConfigs() {
        List<EmployePaieConfigDto> configs = employePaieConfigService.getEmployePaieConfigsForAuthenticatedEmployer();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployePaieConfigDto> getEmployePaieConfigById(@PathVariable Long id) {
        return employePaieConfigService.getEmployePaieConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<EmployePaieConfigDto>> getEmployePaieConfigsByEmployeId(@PathVariable Long employeId) {
        List<EmployePaieConfigDto> configs = employePaieConfigService.getEmployePaieConfigsByEmployeId(employeId);
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/employe/{employeId}/element/{elementPaieId}")
    public ResponseEntity<EmployePaieConfigDto> createEmployePaieConfig(
            @PathVariable Long employeId,
            @PathVariable Long elementPaieId,
            @RequestBody EmployePaieConfig employePaieConfig) { // Reste l'entité si le service attend l'entité en entrée
        EmployePaieConfigDto createdConfig = employePaieConfigService.createEmployePaieConfig(employeId, elementPaieId, employePaieConfig);
        return new ResponseEntity<>(createdConfig, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployePaieConfigDto> updateEmployePaieConfig(@PathVariable Long id,
                                                                        @RequestBody EmployePaieConfig employePaieConfig) { // Reste l'entité
        EmployePaieConfigDto updatedConfig = employePaieConfigService.updateEmployePaieConfig(id, employePaieConfig);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployePaieConfig(@PathVariable Long id) {
        employePaieConfigService.deleteEmployePaieConfig(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-company/search")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<Page<EmployePaieConfigDto>> searchConfigsForMyCompany(
            @RequestParam(required = false) Long employeId,
            @RequestParam(required = false) Long elementPaieId,
            @RequestParam(defaultValue = "all") String status, // "active" ou "all"
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<EmployePaieConfigDto> result = employePaieConfigService.searchConfigsForAuthenticatedEmployer(
                employeId, elementPaieId, status, searchTerm, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(result);
    }
}
