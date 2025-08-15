package com.hades.maalipo.controller;

import com.hades.maalipo.dto.entreprise.EntrepriseParametreRhDto;
import com.hades.maalipo.service.EntrepriseParametreRhService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises/{entrepriseId}/parametres-rh")
public class EntrepriseParametreRhController {

    private final EntrepriseParametreRhService paramService;

    public EntrepriseParametreRhController(EntrepriseParametreRhService paramService) {
        this.paramService = paramService;
    }

    @GetMapping
    public ResponseEntity<List<EntrepriseParametreRhDto>> getAllParams(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(paramService.getAllParamsForEntreprise(entrepriseId));
    }

    @GetMapping("/{cleParametre}")
    public ResponseEntity<EntrepriseParametreRhDto> getParam(@PathVariable Long entrepriseId, @PathVariable String cleParametre) {
        return paramService.getParam(entrepriseId, cleParametre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<EntrepriseParametreRhDto> updateParam(@PathVariable Long id, @RequestBody EntrepriseParametreRhDto dto) {
        EntrepriseParametreRhDto updated = paramService.updateParam(id, dto);
        return ResponseEntity.ok(updated);
    }

}
