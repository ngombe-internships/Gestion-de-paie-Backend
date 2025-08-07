package com.hades.maalipo.controller;

import com.hades.maalipo.dto.ApiResponse;
import com.hades.maalipo.dto.DemandeCongeCreateDto;
import com.hades.maalipo.dto.DemandeCongeResponseDto;
import com.hades.maalipo.model.User;
import com.hades.maalipo.service.DemandeCongeService;
import com.hades.maalipo.service.EmployeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conge")
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;
    private final EmployeService employeService;

    public DemandeCongeController(DemandeCongeService demandeCongeService, EmployeService employeService) {
        this.demandeCongeService = demandeCongeService;
        this.employeService = employeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR')")
    public ResponseEntity<ApiResponse<DemandeCongeResponseDto>> submitDemandeConge(
            @Valid @RequestBody DemandeCongeCreateDto demandeCongeCreateDto) {

        User currentUser = employeService.getAuthenticatedUser();
        DemandeCongeResponseDto newDemande = demandeCongeService.submitDemandeConge(demandeCongeCreateDto, currentUser);

        return new ResponseEntity<>(new ApiResponse<>(
                "Demande de congé soumise avec succès",
                newDemande,
                HttpStatus.CREATED), HttpStatus.CREATED);
    }

    // Récupérer toutes les demandes de congé d'un employé
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DemandeCongeResponseDto>>> getDemandesCongeByEmploye(
            @PathVariable Long employeId) {

        User currentUser = employeService.getAuthenticatedUser();
        List<DemandeCongeResponseDto> demandes = demandeCongeService.getDemandesCongeByEmploye(employeId, currentUser);

        return ResponseEntity.ok(new ApiResponse<>(
                "Demandes de congé récupérées avec succès",
                demandes,
                HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DemandeCongeResponseDto>> getDemandeCongeById(@PathVariable Long id) {
        User currentUser = employeService.getAuthenticatedUser();
        DemandeCongeResponseDto demandeConge = demandeCongeService.getDemandeCongeById(id, currentUser);

        return ResponseEntity.ok(new ApiResponse<>(
                "Demande de congé récupérée avec succès",
                demandeConge,
                HttpStatus.OK));
    }

    @PutMapping("/{id}/approuver")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<DemandeCongeResponseDto>> approveDemandeConge(@PathVariable Long id) {
        User currentUser = employeService.getAuthenticatedUser();
        DemandeCongeResponseDto approvedDemande = demandeCongeService.approveDemandeConge(id, currentUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Demande de congé approuvée avec succès",
                approvedDemande,
                HttpStatus.OK));
    }

    @PutMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<DemandeCongeResponseDto>> rejectDemandeConge(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        User currentUser = employeService.getAuthenticatedUser();
        String motifRejet = payload.get("motifRejet");

        if (motifRejet == null || motifRejet.trim().isEmpty()) {
            throw new IllegalArgumentException("Le motif de rejet est obligatoire.");
        }

        DemandeCongeResponseDto rejectedDemande = demandeCongeService.rejectDemandeConge(id, currentUser.getId(), motifRejet);

        return ResponseEntity.ok(new ApiResponse<>(
                "Demande de congé rejetée avec succès",
                rejectedDemande,
                HttpStatus.OK));
    }

    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR')")
    public ResponseEntity<ApiResponse<DemandeCongeResponseDto>> cancelDemandeConge(@PathVariable Long id) {
        User currentUser = employeService.getAuthenticatedUser();
        DemandeCongeResponseDto cancelledDemande = demandeCongeService.cancelDemandeConge(id, currentUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Demande de congé annulée avec succès",
                cancelledDemande,
                HttpStatus.OK));
    }
}