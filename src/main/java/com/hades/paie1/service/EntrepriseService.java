package com.hades.paie1.service;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.dto.EntrepriseUpdateDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.repository.EntrepriseRepository;
import com.hades.paie1.service.pdf.UnifiedFileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UnifiedFileStorageService unifiedFileStorageService;

    private final AuditLogService auditLogService;
    public EntrepriseService(EntrepriseRepository entrepriseRepository, UnifiedFileStorageService unifiedFileStorageService, AuditLogService auditLogService) {
        this.entrepriseRepository = entrepriseRepository;
        this.unifiedFileStorageService =unifiedFileStorageService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmployeurListDto> getAllEntreprises() {
        return entrepriseRepository.findAll().stream()
                .map(this::convertToEmployeurListDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EntrepriseDto getEntrepriseDtoById(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec l'ID: " + id));

        return convertToEntrepriseDto(entreprise);
    }

    private EntrepriseDto convertToEntrepriseDto(Entreprise entreprise) {
        String logoUrl = null;
        try {
            // Accès sécurisé au LOB
            logoUrl = entreprise.getLogoUrl();
        } catch (Exception e) {
            System.err.println("Erreur d'accès au logoUrl pour l'entreprise " + entreprise.getId() + ": " + e.getMessage());
        }

        int nombreEmployes = entreprise.getEmployes() != null ? entreprise.getEmployes().size():0;

        return EntrepriseDto.builder()
                .id(entreprise.getId())
                .nom(entreprise.getNom())
                .adresseEntreprise(entreprise.getAdresseEntreprise())
                .emailEntreprise(entreprise.getEmailEntreprise())
                .telephoneEntreprise(entreprise.getTelephoneEntreprise())
                .numeroSiret(entreprise.getNumeroSiret())
                .logoUrl(logoUrl)
                .dateCreation(entreprise.getDateCreation())
                .employeurPrincipalId(entreprise.getEmployeurPrincipal() != null ?
                        entreprise.getEmployeurPrincipal().getId() : null)
                .employeurPrincipalUsername(entreprise.getEmployeurPrincipal() != null ?
                        entreprise.getEmployeurPrincipal().getUsername() : null)
                .standardHeuresHebdomadaires(entreprise.getStandardHeuresHebdomadaires())
                .standardJoursOuvrablesHebdomadaires(entreprise.getStandardJoursOuvrablesHebdomadaires())
                .active(entreprise.isActive())
                .nombreEmployes(nombreEmployes)
                .dateCreationSysteme(entreprise.getDateCreationSysteme())
                .dateDerniereMiseAJour(entreprise.getDateDerniereMiseAJour())
                .build();
    }

    private EmployeurListDto convertToEmployeurListDto(Entreprise entreprise) {
        int nombreEmployes = entreprise.getEmployes() != null ? entreprise.getEmployes().size():0;

        return EmployeurListDto.builder()
                .entrepriseId(entreprise.getId())
                .nomEntreprise(entreprise.getNom())
                .dateCreationEntreprise(entreprise.getDateCreation())
                .usernameEmployeur(entreprise.getEmployeurPrincipal() != null ?
                        entreprise.getEmployeurPrincipal().getUsername() : null)
                .active(entreprise.isActive())
                .dateCreationSysteme(entreprise.getDateCreationSysteme())
                .dateDerniereMiseAJour(entreprise.getDateDerniereMiseAJour())
                .nombreEmployes(nombreEmployes)

                .build();
    }

    public void setEntrepriseActiveStatus(Long entrepriseId, boolean isActive){
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(()-> new RessourceNotFoundException("Entreprise non trouvé avec l'ID:" +entrepriseId));

        entreprise.setActive(isActive);
        entrepriseRepository.save(entreprise);
        auditLogService.logAction(
                isActive ? "ACTIVATE_ENTREPRISE" : "DEACTIVATE_ENTREPRISE",
                "Entreprise",
                entreprise.getId(),
                auditLogService.getCurrentUsername(),
                isActive ? "Activation de l'entreprise" : "Désactivation de l'entreprise"
        );
    }

    //mise a jour complete
    public  EntrepriseDto updateEntrepriseByAdmin(Long id, EntrepriseDto entrepriseDto, MultipartFile logoFile) throws IOException {
        Entreprise existingEntreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvé avec l'ID: " +id));
        existingEntreprise.setNom(entrepriseDto.getNom());
        existingEntreprise.setAdresseEntreprise(entrepriseDto.getAdresseEntreprise());
        existingEntreprise.setEmailEntreprise(entrepriseDto.getEmailEntreprise());
        existingEntreprise.setNumeroSiret(entrepriseDto.getNumeroSiret());
        existingEntreprise.setDateCreation(entrepriseDto.getDateCreation());
        existingEntreprise.setActive(entrepriseDto.isActive());

        if(logoFile != null && !logoFile.isEmpty()){
            if(existingEntreprise.getLogoUrl() != null && !existingEntreprise.getLogoUrl().isEmpty()) {
                unifiedFileStorageService.deleteFile(existingEntreprise.getLogoUrl());
            }
            String newLogoUrl = unifiedFileStorageService.saveFile(logoFile);
            existingEntreprise.setLogoUrl(newLogoUrl);
        } else if (entrepriseDto.getLogoUrl() == null || entrepriseDto.getLogoUrl().isEmpty()) {
            if(existingEntreprise.getLogoUrl() != null && !existingEntreprise.getLogoUrl().isEmpty()){
                unifiedFileStorageService.deleteFile(existingEntreprise.getLogoUrl());
            }
            existingEntreprise.setLogoUrl(null);
        }
        Entreprise updateEntreprise = entrepriseRepository.save(existingEntreprise);
        auditLogService.logAction(
                "UPDATE_ENTREPRISE",
                "Entreprise",
                updateEntreprise.getId(),
                auditLogService.getCurrentUsername(),
                "Mise à jour (admin) des infos de l'entreprise"
        );
        return convertToEntrepriseDto(updateEntreprise);
    }

    //mise a jour partielle
    public EntrepriseDto updateEntrepriseByEmployer(Long entrepriseId, EntrepriseUpdateDto updateDto, MultipartFile logoFile) throws IOException {
        Entreprise existingEntreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec l'ID: " + entrepriseId));
        // Mise à jour des champs autorisés pour l'employeur
        Optional.ofNullable(updateDto.getNom()).ifPresent(existingEntreprise::setNom); // Si le nom peut être modifié par l'employeur
        Optional.ofNullable(updateDto.getAdresseEntreprise()).ifPresent(existingEntreprise::setAdresseEntreprise);
        Optional.ofNullable(updateDto.getEmailEntreprise()).ifPresent(existingEntreprise::setEmailEntreprise);
        Optional.ofNullable(updateDto.getTelephoneEntreprise()).ifPresent(existingEntreprise::setTelephoneEntreprise);

        // Gestion du logo par l'employeur
        if (logoFile != null && !logoFile.isEmpty()) {
            // Supprimer l'ancien logo si nécessaire
            if (existingEntreprise.getLogoUrl() != null && !existingEntreprise.getLogoUrl().isEmpty()) {
                unifiedFileStorageService.deleteFile(existingEntreprise.getLogoUrl());
            }
            String newLogoUrl = unifiedFileStorageService.saveFile(logoFile);
            existingEntreprise.setLogoUrl(newLogoUrl);
        } else if (updateDto.getLogoUrl() == null || updateDto.getLogoUrl().isEmpty()) {
            // Si le DTO indique que le logo doit être supprimé (par exemple, si le frontend envoie logoUrl vide pour supprimer)
            if (existingEntreprise.getLogoUrl() != null && !existingEntreprise.getLogoUrl().isEmpty()) {
                unifiedFileStorageService.deleteFile(existingEntreprise.getLogoUrl());
            }
            existingEntreprise.setLogoUrl(null);
        }
        // Si logoFile est null et updateDto.getLogoUrl() n'est pas null, on conserve l'ancien logo.

        Entreprise updatedEntreprise = entrepriseRepository.save(existingEntreprise);

        auditLogService.logAction(
                "UPDATE_ENTREPRISE",
                "Entreprise",
                updatedEntreprise.getId(),
                auditLogService.getCurrentUsername(),
                "Mise à jour des infos de l'entreprise"
        );
        return convertToEntrepriseDto(updatedEntreprise);
    }


}


