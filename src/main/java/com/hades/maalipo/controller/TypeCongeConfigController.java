package com.hades.maalipo.controller;

import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.conge.TypeCongeConfigDTO;
import com.hades.maalipo.dto.conge.TypeCongeConfigResponseDto;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.TypeCongeConfig;
import com.hades.maalipo.model.User;
import com.hades.maalipo.service.EmployeService;
import com.hades.maalipo.service.conge.TypeCongeConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/type-conge-config")
public class TypeCongeConfigController {

    private final TypeCongeConfigService typeCongeConfigService;
    private final EmployeService employeService;

    public TypeCongeConfigController(TypeCongeConfigService typeCongeConfigService,
                                     EmployeService employeService) {
        this.typeCongeConfigService = typeCongeConfigService;
        this.employeService = employeService;
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TypeCongeConfigDTO>>> getAllConfigs() {
        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        // ✅ CHANGEMENT : Récupérer TOUTES les configurations (actives ET inactives)
        List<TypeCongeConfigDTO> configs = typeCongeConfigService
                .getAllConfigsByEntreprise(currentUser.getEntreprise().getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Configurations des types de congés récupérées avec succès",
                configs,
                HttpStatus.OK));
    }

    /**
     * Récupérer toutes les configurations actives de types de congés pour l'entreprise courante
     */
    @GetMapping("/actives")
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TypeCongeConfigDTO>>> getActiveConfigs() {
        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        List<TypeCongeConfigDTO> configs = typeCongeConfigService
                .getActiveConfigsByEntreprise(currentUser.getEntreprise().getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Configurations des types de congés récupérées avec succès",
                configs,
                HttpStatus.OK));
    }

    /**
     * Récupérer une configuration spécifique par type de congé
     */
    @GetMapping("/type/{typeConge}")
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<ApiResponse<TypeCongeConfigDTO>> getConfigByType(
            @PathVariable TypeConge typeConge) {

        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        Optional<TypeCongeConfigDTO> config = typeCongeConfigService
                .getActiveConfigByEntrepriseAndType(currentUser.getEntreprise().getId(), typeConge);

        if (config.isPresent()) {
            return ResponseEntity.ok(new ApiResponse<>(
                    "Configuration trouvée",
                    config.get(),
                    HttpStatus.OK));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>("Configuration non trouvée pour le type: " + typeConge,
                            null, HttpStatus.NOT_FOUND));
        }
    }

    /**
     * Initialiser les configurations par défaut pour une entreprise
     * (Appelé automatiquement lors de la création d'entreprise, mais peut être réexécuté)
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultConfigs() {
        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        typeCongeConfigService.initializeDefaultConfigsForEntreprise(
                currentUser.getEntreprise().getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Configurations par défaut initialisées avec succès",
                null,
                HttpStatus.OK));
    }

    /**
     * Mettre à jour une configuration existante
     */
    @PutMapping("/{configId}")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<TypeCongeConfig>> updateConfig(
            @PathVariable Long configId,
            @Valid @RequestBody TypeCongeConfigUpdateRequest updateRequest) {

        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        TypeCongeConfig updates = new TypeCongeConfig();
        updates.setDureeMaximaleJours(updateRequest.getDureeMaximaleJours());
        updates.setDelaiPreavisJours(updateRequest.getDelaiPreavisJours());
        updates.setPourcentageRemuneration(updateRequest.getPourcentageRemuneration());
        updates.setDocumentsRequis(updateRequest.getDocumentsRequis());
        updates.setConditionsAttribution(updateRequest.getConditionsAttribution());
        updates.setCumulAutorise(updateRequest.getCumulAutorise());
        updates.setActif(updateRequest.getActif());

        try {
            TypeCongeConfig updatedConfig = typeCongeConfigService.updateConfig(
                    configId, updates, currentUser.getEntreprise().getId());

            return ResponseEntity.ok(new ApiResponse<>(
                    "Configuration mise à jour avec succès",
                    updatedConfig,
                    HttpStatus.OK));

        } catch (RessourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(e.getMessage(), null, HttpStatus.NOT_FOUND));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(e.getMessage(), null, HttpStatus.FORBIDDEN));
        }
    }

    /**
     * Activer/Désactiver un type de congé
     */
    @PatchMapping("/{configId}/toggle-active")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<TypeCongeConfigResponseDto>> toggleActiveStatus(
            @PathVariable Long configId) {

        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            null, HttpStatus.BAD_REQUEST));
        }

        try {
            TypeCongeConfigResponseDto updatedConfig = typeCongeConfigService.toggleActiveStatus(
                    configId, currentUser.getEntreprise().getId());

            return ResponseEntity.ok(new ApiResponse<>(
                    "Statut de la configuration mis à jour",
                    updatedConfig,
                    HttpStatus.OK));

        } catch (RessourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(e.getMessage(), null, HttpStatus.NOT_FOUND));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(e.getMessage(), null, HttpStatus.FORBIDDEN));
        }
    }

    /**
     * Vérifier si une configuration existe pour un type donné
     */
    @GetMapping("/exists/{typeConge}")
    @PreAuthorize("hasAnyRole('EMPLOYEUR', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<ApiResponse<Boolean>> configExists(@PathVariable TypeConge typeConge) {
        User currentUser = employeService.getAuthenticatedUser();

        if (currentUser.getEntreprise() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Utilisateur non associé à une entreprise",
                            false, HttpStatus.BAD_REQUEST));
        }

        boolean exists = typeCongeConfigService.configExists(
                currentUser.getEntreprise().getId(), typeConge);

        return ResponseEntity.ok(new ApiResponse<>(
                "Vérification effectuée",
                exists,
                HttpStatus.OK));
    }

    /**
     * Classe interne pour les requêtes de mise à jour
     */
    public static class TypeCongeConfigUpdateRequest {
        private Integer dureeMaximaleJours;
        private Integer delaiPreavisJours;
        private BigDecimal pourcentageRemuneration;  // Changé en BigDecimal
        private String documentsRequis;
        private String conditionsAttribution;
        private Boolean cumulAutorise;
        private Boolean actif;

        // Getters et Setters
        public Integer getDureeMaximaleJours() { return dureeMaximaleJours; }
        public void setDureeMaximaleJours(Integer dureeMaximaleJours) { this.dureeMaximaleJours = dureeMaximaleJours; }

        public Integer getDelaiPreavisJours() { return delaiPreavisJours; }
        public void setDelaiPreavisJours(Integer delaiPreavisJours) { this.delaiPreavisJours = delaiPreavisJours; }

        public BigDecimal getPourcentageRemuneration() { return pourcentageRemuneration; }
        public void setPourcentageRemuneration(BigDecimal pourcentageRemuneration) { this.pourcentageRemuneration = pourcentageRemuneration; }

        public String getDocumentsRequis() { return documentsRequis; }
        public void setDocumentsRequis(String documentsRequis) { this.documentsRequis = documentsRequis; }

        public String getConditionsAttribution() { return conditionsAttribution; }
        public void setConditionsAttribution(String conditionsAttribution) { this.conditionsAttribution = conditionsAttribution; }

        public Boolean getCumulAutorise() { return cumulAutorise; }
        public void setCumulAutorise(Boolean cumulAutorise) { this.cumulAutorise = cumulAutorise; }

        public Boolean getActif() { return actif; }
        public void setActif(Boolean actif) { this.actif = actif; }
    }
}