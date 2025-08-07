// src/main/java/com/hades/paie1/controller/EmployeurEntrepriseController.java
package com.hades.maalipo.controller;

import com.hades.maalipo.dto.ApiResponse;
import com.hades.maalipo.dto.EntrepriseDto;
import com.hades.maalipo.dto.EntrepriseUpdateDto; // Importez le DTO de mise à jour pour l'employeur
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.UserRepository; // Pour récupérer l'utilisateur authentifié
import com.hades.maalipo.service.EntrepriseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/employer/entreprise")
@CrossOrigin(origins = "http://localhost:4200") // Adaptez si nécessaire
public class EmployeurEntrepriseController {

    private final EntrepriseService entrepriseService;
    private final UserRepository userRepository; // Pour accéder aux informations de l'utilisateur authentifié

    public EmployeurEntrepriseController(EntrepriseService entrepriseService, UserRepository userRepository) {
        this.entrepriseService = entrepriseService;
        this.userRepository = userRepository;
    }

    //Endpoint pour récupérer les informations de l'entreprise de l'employeur authentifié.
     
    @GetMapping("/my-company")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<EntrepriseDto>> getMyCompanyDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur non trouvé: " + username));

        if (currentUser.getRole() != Role.EMPLOYEUR) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Accès refusé. Seul un employeur peut accéder à cette ressource.", null, HttpStatus.FORBIDDEN),
                    HttpStatus.FORBIDDEN
            );
        }

        if (currentUser.getEntreprise() == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Aucune entreprise associée à cet employeur.", null, HttpStatus.NOT_FOUND),
                    HttpStatus.NOT_FOUND
            );
        }

        EntrepriseDto entrepriseDto = entrepriseService.getEntrepriseDtoById(currentUser.getEntreprise().getId());
        return new ResponseEntity<>(
                new ApiResponse<>("Détails de votre entreprise récupérés avec succès.", entrepriseDto, HttpStatus.OK),
                HttpStatus.OK
        );
    }

    /**
     * Endpoint pour la modification partielle des informations de l'entreprise par l'employeur.
     */
    @PatchMapping(value = "/my-company", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<EntrepriseDto>> updateMyCompany(
            @RequestPart("entreprise") EntrepriseUpdateDto updateDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RessourceNotFoundException("Utilisateur non trouvé: " + username));


            if (currentUser.getEntreprise() == null) {
                return new ResponseEntity<>(
                        new ApiResponse<>("Aucune entreprise associée à cet employeur.", null, HttpStatus.BAD_REQUEST),
                        HttpStatus.BAD_REQUEST
                );
            }

            EntrepriseDto updatedEntreprise = entrepriseService.updateEntrepriseByEmployer(
                    currentUser.getEntreprise().getId(), updateDto, logoFile);

            return new ResponseEntity<>(
                    new ApiResponse<>("Informations de votre entreprise mises à jour avec succès.", updatedEntreprise, HttpStatus.OK),
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
}