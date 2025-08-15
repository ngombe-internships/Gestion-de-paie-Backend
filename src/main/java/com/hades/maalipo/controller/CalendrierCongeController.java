package com.hades.maalipo.controller;

import com.hades.maalipo.dto.conge.AbsenceJourneeDto;
import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.conge.CalendrierMoisDto;
import com.hades.maalipo.dto.conge.EffectifJournalierDto;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.User;
import com.hades.maalipo.service.EmployeService;
import com.hades.maalipo.service.conge.CalendrierCongeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/conge/calendrier")
@PreAuthorize("hasRole('EMPLOYEUR')") // Seuls les employeurs peuvent accéder au calendrier
public class CalendrierCongeController {

    private final CalendrierCongeService calendrierCongeService;
    private final EmployeService employeService;

    public CalendrierCongeController(CalendrierCongeService calendrierCongeService,
                                     EmployeService employeService) {
        this.calendrierCongeService = calendrierCongeService;
        this.employeService = employeService;
    }

    @GetMapping("/{annee}/{mois}")
    public ResponseEntity<ApiResponse<List<AbsenceJourneeDto>>> getAbsencesMensuelles(
            @PathVariable int annee, @PathVariable int mois) {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        List<AbsenceJourneeDto> absences = calendrierCongeService.getAbsencesMensuelles(entreprise, annee, mois);

        return ResponseEntity.ok(new ApiResponse<>(
                "Calendrier des absences récupéré avec succès",
                absences,
                HttpStatus.OK));
    }

    @GetMapping("/complet/{annee}/{mois}")
    public ResponseEntity<ApiResponse<CalendrierMoisDto>> getCalendrierMensuel(
            @PathVariable int annee, @PathVariable int mois) {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        CalendrierMoisDto calendrier = calendrierCongeService.getCalendrierMensuel(entreprise, annee, mois);

        return ResponseEntity.ok(new ApiResponse<>(
                "Calendrier mensuel récupéré avec succès",
                calendrier,
                HttpStatus.OK));
    }

    @GetMapping("/demandes-attente")
    public ResponseEntity<ApiResponse<List<AbsenceJourneeDto>>> getDemandesEnAttente() {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        List<AbsenceJourneeDto> demandesEnAttente = calendrierCongeService.getDemandesEnAttente(entreprise);

        return ResponseEntity.ok(new ApiResponse<>(
                "Demandes en attente récupérées avec succès",
                demandesEnAttente,
                HttpStatus.OK));
    }

    @GetMapping("/conflits-potentiels")
    public ResponseEntity<ApiResponse<List<EffectifJournalierDto>>> detecterConflitsPotentiels(
            @RequestParam(defaultValue = "30") int nbJours) {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        List<EffectifJournalierDto> conflits = calendrierCongeService.detecterConflitsPotentiels(entreprise, nbJours);

        return ResponseEntity.ok(new ApiResponse<>(
                "Conflits potentiels détectés avec succès",
                conflits,
                HttpStatus.OK));
    }

    @GetMapping("/analyser-impact")
    public ResponseEntity<ApiResponse<List<EffectifJournalierDto>>> analyserImpactDemandeConge(
            @RequestParam String dateDebut, @RequestParam String dateFin) {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);

        List<EffectifJournalierDto> impact = calendrierCongeService.analyserImpactDemandeConge(entreprise, debut, fin);

        return ResponseEntity.ok(new ApiResponse<>(
                "Analyse d'impact réalisée avec succès",
                impact,
                HttpStatus.OK));
    }

    @GetMapping("/export/{annee}")
    public ResponseEntity<String> exporterCalendrier(@PathVariable int annee) {
        User currentUser = employeService.getAuthenticatedUser();
        Entreprise entreprise = currentUser.getEntreprise();

        String icalData = calendrierCongeService.genererDonneesExportCalendrier(entreprise, annee);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"calendar-" + annee + ".ics\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(icalData);
    }
}