package com.hades.maalipo.controller;

import com.hades.maalipo.dto.ElementPaieDto;
import com.hades.maalipo.model.ElementPaie;
import com.hades.maalipo.service.ElementPaieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/element-paie")
public class ElementPaieController {

    private final ElementPaieService elementPaieService;

    public ElementPaieController(ElementPaieService elementPaieService) {
        this.elementPaieService = elementPaieService;
    }

    @GetMapping
    public ResponseEntity<List<ElementPaieDto>> getAllElementsPaie() {
        List<ElementPaieDto> elements = elementPaieService.getAllElementsPaie();
        return ResponseEntity.ok(elements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElementPaieDto> getElementPaieById(@PathVariable Long id) {
        return elementPaieService.getElementPaieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ElementPaieDto> createElementPaie(@RequestBody ElementPaie elementPaie) { // Conserve l'entité en @RequestBody pour l'instant
        ElementPaieDto createdElement = elementPaieService.createElementPaie(elementPaie);
        return new ResponseEntity<>(createdElement, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElementPaieDto> updateElementPaie(@PathVariable Long id, @RequestBody ElementPaie elementPaie) { // Conserve l'entité en @RequestBody pour l'instant
        ElementPaieDto updatedElement = elementPaieService.updateElementPaie(id, elementPaie);
        return ResponseEntity.ok(updatedElement);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteElementPaie(@PathVariable Long id) {
        elementPaieService.deleteElementPaie(id);
        return ResponseEntity.noContent().build();
    }

}