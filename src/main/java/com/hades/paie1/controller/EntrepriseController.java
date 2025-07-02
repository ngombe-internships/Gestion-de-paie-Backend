package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.repository.EntrepriseRepository;
import com.hades.paie1.service.EntrepriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/entreprises")
public class EntrepriseController {



    private final EntrepriseService entrepriseService;
    public EntrepriseController (EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployeurListDto>>> getEmployersList() {
       List<EmployeurListDto> entreprises = entrepriseService.getAllEntreprises();
       ApiResponse<List<EmployeurListDto>> response = new ApiResponse<>(
               "Liste de toute les entreprise",
               entreprises,
               HttpStatus.OK
       );
       return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EntrepriseDto>> getEntrepriseDetails(@PathVariable Long id) {
        EntrepriseDto entreprise = entrepriseService.getEntrepriseById(id);

        ApiResponse<EntrepriseDto> response = new ApiResponse<>(
                "Détails de l'entreprise récupérés avec succès",
                entreprise,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}