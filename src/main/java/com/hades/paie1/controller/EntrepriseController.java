package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.dto.EntrepriseUpdateDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.repository.EntrepriseRepository;
import com.hades.paie1.service.EntrepriseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/entreprises")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }



    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // Permettre aux employeurs de voir leur entreprise
    public ResponseEntity<ApiResponse<EntrepriseDto>> getEntrepriseById(@PathVariable Long id) {
        try {
            EntrepriseDto entreprise = entrepriseService.getEntrepriseDtoById(id);
            return new ResponseEntity<>(
                    new ApiResponse<>("Détails de l'entreprise récupérés avec succès.", entreprise, HttpStatus.OK),
                    HttpStatus.OK
            );
        } catch (RessourceNotFoundException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.NOT_FOUND),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Erreur inattendue lors de la récupération de l'entreprise : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> toggleEntrepriseStatus(@PathVariable Long id, @RequestParam boolean active) {
        try {
            entrepriseService.setEntrepriseActiveStatus(id, active);
            String message = active ? "Entreprise activée avec succès." : "Entreprise désactivée avec succès.";
            return new ResponseEntity<>(
                    new ApiResponse<>(message, null, HttpStatus.OK),
                    HttpStatus.OK
            );
        } catch (RessourceNotFoundException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.NOT_FOUND), // Correction: NOT_FOUND au lieu de NO_CONTENT
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Erreur lors de la mise à jour du statut de l'entreprise: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Endpoint pour modification complète par ADMIN
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EntrepriseDto>> updateEntrepriseByAdmin(
            @PathVariable Long id,
            @RequestPart("entreprise") EntrepriseDto entrepriseDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {
        try {
            EntrepriseDto updatedEntreprise = entrepriseService.updateEntrepriseByAdmin(id, entrepriseDto, logoFile);
            return new ResponseEntity<>(
                    new ApiResponse<>("Informations de l'entreprise mises à jour par l'admin avec succès.", updatedEntreprise, HttpStatus.OK),
                    HttpStatus.OK
            );
        } catch (RessourceNotFoundException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.NOT_FOUND),
                    HttpStatus.NOT_FOUND
            );
        } catch (IOException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Erreur lors du traitement du logo : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Erreur inattendue lors de la mise à jour de l'entreprise : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EmployeurListDto>>> getEmployersList1(
            @RequestParam(defaultValue = "") String nomEntreprise,
            @RequestParam(defaultValue = "") String usernameEmployeur,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<EmployeurListDto> entreprises = entrepriseService.searchEntreprises(nomEntreprise, usernameEmployeur, active, pageable);
        ApiResponse<Page<EmployeurListDto>> response = new ApiResponse<>(
                "Liste filtrée et paginée",
                entreprises,
                HttpStatus.OK
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getEmployeurCounts(
            @RequestParam(defaultValue = "") String nomEntreprise,
            @RequestParam(defaultValue = "") String usernameEmployeur
    ) {
        Map<String, Long> result = entrepriseService.countActifsInactifs(nomEntreprise, usernameEmployeur);
        return ResponseEntity.ok(result);
    }


}