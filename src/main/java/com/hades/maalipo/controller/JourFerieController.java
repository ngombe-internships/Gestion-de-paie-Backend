package com.hades.maalipo.controller;

import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.conge.JourFerieDto;
import com.hades.maalipo.dto.conge.JourFerieRequestDto;
import com.hades.maalipo.dto.conge.JourFerieUpdateDto;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.JourFerie;
import com.hades.maalipo.service.conge.JourFerieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jours-feries")
public class JourFerieController {

    private final JourFerieService jourferieService;

    public JourFerieController(JourFerieService jourferieService) {
        this.jourferieService = jourferieService;
    }

    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<JourFerieDto>>> getAllJourFeries() {
        List<JourFerieDto> jours = jourferieService.getAllJoursFeriesDto();

        ApiResponse<List<JourFerieDto>> response = new ApiResponse<>(
                "Liste des jours fériés récupérée avec succès",
                jours,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'EMPLOYEUR')")
    public ResponseEntity<ApiResponse<JourFerie>> getJourFerieById(@PathVariable Long id) {
        Optional<JourFerie> jour = jourferieService.getJourFerieById(id);

        if (jour.isPresent()) {
            ApiResponse<JourFerie> response = new ApiResponse<>(
                    "Jour férié trouvé",
                    jour.get(),
                    HttpStatus.OK
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<JourFerie> errorResponse = new ApiResponse<>(
                    "Jour férié non trouvé avec ID: " + id,
                    null,
                    HttpStatus.NOT_FOUND
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<JourFerie>> addJourFerie(@RequestBody JourFerieRequestDto jourFerieDto) {
        try {
            JourFerie newjour = jourferieService.addJourFerie(jourFerieDto);

            ApiResponse<JourFerie> response = new ApiResponse<>(
                    "Jour férié ajouté avec succès",
                    newjour,
                    HttpStatus.CREATED
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<JourFerie> errorResponse = new ApiResponse<>(
                    "Erreur lors de l'ajout du jour férié: " + e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }


    @PreAuthorize("hasRole('EMPLOYEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JourFerieUpdateDto>> updateJourFerie(@PathVariable Long id, @RequestBody JourFerie jourFerie) {
        JourFerieUpdateDto updatedJour = jourferieService.updateJourFerie(id, jourFerie);

        ApiResponse<JourFerieUpdateDto> response = new ApiResponse<>(
                "Mise à jour du jour férié effectuée avec succès",
                updatedJour,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('EMPLOYEUR')")
    @GetMapping("/date/{date}")
    public ResponseEntity<JourFerie> getJourFerieByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        JourFerie jourFerie = jourferieService.getJoutFerieByDate(localDate)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun jour férié trouvé pour la date : " + date));
        return ResponseEntity.ok(jourFerie);
    }

    @PreAuthorize("hasRole('EMPLOYEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJourFerie(@PathVariable Long id) {
        jourferieService.deleteId(id);

        ApiResponse<Void> response = new ApiResponse<>(
                "Jour férié supprimé avec succès",
                null,
                HttpStatus.NO_CONTENT
        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}