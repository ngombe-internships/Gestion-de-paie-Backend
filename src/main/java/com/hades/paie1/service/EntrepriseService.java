package com.hades.paie1.service;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.repository.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseService(EntrepriseRepository entrepriseRepository) {
        this.entrepriseRepository = entrepriseRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeurListDto> getAllEntreprises() {
        return entrepriseRepository.findAll().stream()
                .map(this::convertToEmployeurListDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EntrepriseDto getEntrepriseById(Long id) {
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
                .build();
    }

    private EmployeurListDto convertToEmployeurListDto(Entreprise entreprise) {
        return EmployeurListDto.builder()
                .entrepriseId(entreprise.getId())
                .nomEntreprise(entreprise.getNom())
                .dateCreationEntreprise(entreprise.getDateCreation())
                .usernameEmployeur(entreprise.getEmployeurPrincipal() != null ?
                        entreprise.getEmployeurPrincipal().getUsername() : null)
                .build();
    }
}