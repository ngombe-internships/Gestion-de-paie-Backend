package com.hades.paie1.controller;

import com.hades.paie1.dto.TemplateElementPaieConfigDto;
import com.hades.paie1.service.TemplateElementPaieConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/template-element-configs")
public class TemplateElementPaieConfigController {

    private final TemplateElementPaieConfigService templateElementPaieConfigService;

    public TemplateElementPaieConfigController(TemplateElementPaieConfigService templateElementPaieConfigService) {
        this.templateElementPaieConfigService = templateElementPaieConfigService;
    }

    @GetMapping
    public ResponseEntity<List<TemplateElementPaieConfigDto>> getAllTemplateElementPaieConfigs() {
        List<TemplateElementPaieConfigDto> configs = templateElementPaieConfigService.getAllTemplateElementPaieConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateElementPaieConfigDto> getTemplateElementPaieConfigById(@PathVariable Long id) {
        return templateElementPaieConfigService.getTemplateElementPaieConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint pour obtenir les configurations d'un template spécifique
    @GetMapping("/by-template/{templateId}")
    public ResponseEntity<List<TemplateElementPaieConfigDto>> getTemplateElementPaieConfigsByBulletinTemplateId(@PathVariable Long templateId) {
        List<TemplateElementPaieConfigDto> configs = templateElementPaieConfigService.getTemplateElementPaieConfigsByBulletinTemplateId(templateId);
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/template/{templateId}/element/{elementPaieId}")
    public ResponseEntity<TemplateElementPaieConfigDto> createTemplateElementPaieConfig(
            @PathVariable Long templateId,
            @PathVariable Long elementPaieId,
            @RequestBody TemplateElementPaieConfigDto configDto) {
        TemplateElementPaieConfigDto createdConfig = templateElementPaieConfigService.createTemplateElementPaieConfig(templateId, elementPaieId, configDto);
        return new ResponseEntity<>(createdConfig, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateElementPaieConfigDto> updateTemplateElementPaieConfig(
            @PathVariable Long id,
            @RequestBody TemplateElementPaieConfigDto configDto) {
        TemplateElementPaieConfigDto updatedConfig = templateElementPaieConfigService.updateTemplateElementPaieConfig(id, configDto);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplateElementPaieConfig(@PathVariable Long id) {
        templateElementPaieConfigService.deleteTemplateElementPaieConfig(id);
        return ResponseEntity.noContent().build();
    }
}
