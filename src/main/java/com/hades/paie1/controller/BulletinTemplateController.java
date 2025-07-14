package com.hades.paie1.controller;

import com.hades.paie1.dto.BulletinTemplateDto;
import com.hades.paie1.dto.TemplateElementPaieConfigDto;
import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.TemplateElementPaieConfig;
import com.hades.paie1.service.BulletinTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class BulletinTemplateController {

    private final BulletinTemplateService bulletinTemplateService;

    public BulletinTemplateController(BulletinTemplateService bulletinTemplateService) {
        this.bulletinTemplateService = bulletinTemplateService;
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<BulletinTemplateDto>> getTemplatesByEntreprise(@PathVariable Long entrepriseId) {
        List<BulletinTemplateDto> templates = bulletinTemplateService.getTemplatesByEntreprise(entrepriseId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletinTemplateDto> getBulletinTemplateById(@PathVariable Long id) {
        return bulletinTemplateService.getBulletinTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<BulletinTemplateDto> createBulletinTemplate(
            @PathVariable Long entrepriseId,
            @RequestBody BulletinTemplateDto bulletinTemplateDto) {
        BulletinTemplateDto createdTemplate = bulletinTemplateService.createBulletinTemplate(entrepriseId, bulletinTemplateDto);
        return new ResponseEntity<>(createdTemplate, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BulletinTemplateDto> updateBulletinTemplate(
            @PathVariable Long id,
            @RequestBody BulletinTemplateDto bulletinTemplateDto) {
        BulletinTemplateDto updatedTemplate = bulletinTemplateService.updateBulletinTemplate(id, bulletinTemplateDto);
        return ResponseEntity.ok(updatedTemplate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBulletinTemplate(@PathVariable Long id) {
        bulletinTemplateService.deleteBulletinTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/elements")
    public ResponseEntity<TemplateElementPaieConfigDto> addOrUpdateElementToTemplate(
            @PathVariable Long templateId,
            @RequestBody TemplateElementPaieConfigDto configDto) {
        TemplateElementPaieConfigDto updatedConfig = bulletinTemplateService.addOrUpdateElementToTemplate(templateId, configDto);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/{templateId}/elements/{elementPaieId}")
    public ResponseEntity<Void> removeElementFromTemplate(
            @PathVariable Long templateId,
            @PathVariable Long elementPaieId) {
        bulletinTemplateService.removeElementFromTemplate(templateId, elementPaieId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<Void> setTemplateAsDefault(@PathVariable Long id) {
        bulletinTemplateService.setDefaultTemplate(id);
        return ResponseEntity.ok().build();
    }
}
