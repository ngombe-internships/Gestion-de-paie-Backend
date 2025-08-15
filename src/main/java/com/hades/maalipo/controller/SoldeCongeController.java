package com.hades.maalipo.controller;

import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.conge.HistoriqueCongeDto;
import com.hades.maalipo.dto.conge.SoldeCongeDto;
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.service.EmployeService;
import com.hades.maalipo.service.conge.SoldeCongeService;
import com.hades.maalipo.repository.SoldeHistoriqueRepository;
import com.hades.maalipo.utils.JourOuvrableCalculatorUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conge/soldes")
public class SoldeCongeController {

    private final SoldeCongeService soldeCongeService;
    private final EmployeService employeService;
    private final SoldeHistoriqueRepository soldeHistoriqueRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final JourOuvrableCalculatorUtil ouvrableCalculatorUtil;

    public SoldeCongeController(
            SoldeCongeService soldeCongeService,
            EmployeService employeService,
            SoldeHistoriqueRepository soldeHistoriqueRepository,
            DemandeCongeRepository demandeCongeRepository,
            JourOuvrableCalculatorUtil ouvrableCalculatorUtil) {
        this.soldeCongeService = soldeCongeService;
        this.employeService = employeService;
        this.soldeHistoriqueRepository = soldeHistoriqueRepository;
        this.demandeCongeRepository = demandeCongeRepository;
        this.ouvrableCalculatorUtil = ouvrableCalculatorUtil;
    }

    // Endpoint pour que l'employé voie son propre solde
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<ApiResponse<SoldeCongeDto>> getMySolde() {
        User currentUser = employeService.getAuthenticatedUser();
        Employe employe = currentUser.getEmploye();

        if (employe == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>("Utilisateur non associé à un employé", null, HttpStatus.BAD_REQUEST)
            );
        }

        SoldeCongeDto soldeDto = soldeCongeService.construireSoldeDto(employe);
        return ResponseEntity.ok(new ApiResponse<>(
                "Solde de congés récupéré avec succès",
                soldeDto,
                HttpStatus.OK
        ));
    }

    // Endpoint pour que l'employeur voie le solde d'un employé spécifique
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<SoldeCongeDto>> getEmployeSolde(@PathVariable Long employeId) {
        User currentUser = employeService.getAuthenticatedUser();

        // Vérifier que l'employé appartient à l'entreprise de l'employeur
        Employe employe = employeService.findEmployeById(employeId);

        if (employe == null) {
            return ResponseEntity.notFound().build();
        }

        if (!employe.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>("Accès refusé: l'employé n'appartient pas à votre entreprise",
                            null, HttpStatus.FORBIDDEN)
            );
        }

        SoldeCongeDto soldeDto = soldeCongeService.construireSoldeDto(employe);
        return ResponseEntity.ok(new ApiResponse<>(
                "Solde de congés récupéré avec succès",
                soldeDto,
                HttpStatus.OK
        ));
    }

    // Endpoint pour que l'employeur voie tous les soldes de son entreprise
    @GetMapping("/entreprise")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<List<SoldeCongeDto>>> getAllEmployesSolde() {
        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>("Utilisateur non associé à une entreprise", null, HttpStatus.BAD_REQUEST)
            );
        }

        List<Employe> employes = employeService.findByEntreprise(currentUser.getEntreprise());

        List<SoldeCongeDto> soldes = employes.stream()
                .map(soldeCongeService::construireSoldeDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(
                "Soldes de congés récupérés avec succès",
                soldes,
                HttpStatus.OK
        ));
    }



    @GetMapping("/historique/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'EMPLOYE')")
    public ResponseEntity<ApiResponse<List<HistoriqueCongeDto>>> getHistoriqueConges(@PathVariable Long employeId) {
        User currentUser = employeService.getAuthenticatedUser();

        // Vérification des permissions
        if (currentUser.getRole() == Role.EMPLOYE && !currentUser.getEmploye().getId().equals(employeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse<>("Accès refusé", null, HttpStatus.FORBIDDEN)
            );
        }

        if (currentUser.getRole() == Role.EMPLOYEUR) {
            Employe employe = employeService.findEmployeById(employeId);
            if (!employe.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ApiResponse<>("Accès refusé", null, HttpStatus.FORBIDDEN)
                );
            }
        }

        // Récupérer l'historique
        List<HistoriqueCongeDto> historique = soldeCongeService.getHistoriqueConges(employeId);

        return ResponseEntity.ok(new ApiResponse<>(
                "Historique des congés récupéré avec succès",
                historique,
                HttpStatus.OK
        ));
    }



}