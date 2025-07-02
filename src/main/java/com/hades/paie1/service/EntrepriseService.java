package com.hades.paie1.service;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.repository.EntrepriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EntrepriseService {
    private final EntrepriseRepository entrepriseRepository;

    @Autowired
    public EntrepriseService (EntrepriseRepository entrepriseRepository) {
        this.entrepriseRepository = entrepriseRepository;
    }

    private EntrepriseDto convertTodo (Entreprise entreprise) {
        if (entreprise == null) {
            return null;
        }
        return EntrepriseDto.builder()
                .id(entreprise.getId())
                .nom(entreprise.getNom())
                .adresseEntreprise(entreprise.getAdresseEntreprise())
                .emailEntreprise(entreprise.getEmailEntreprise())
                .telephoneEntreprise(entreprise.getTelephoneEntreprise())
                .numeroSiret(entreprise.getNumeroSiret())
                .dateCreation(entreprise.getDateCreation())
                .logoUrl(entreprise.getLogoUrl())
                .build();
    }

    public EntrepriseDto getEntrepriseById(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(()-> new RessourceNotFoundException("Entreprise non trouve avec l'id : " +id));
        return convertTodo(entreprise);
    }

    public List <EmployeurListDto> getAllEntreprises(){

        List<Entreprise> entreprises = entrepriseRepository.findAllWithEmployeurPrincipalLoaded(Sort.by(Sort.Direction.ASC,"dateCreation"));

              return   entreprises.stream()
                      .map(entreprise -> {
                    String usernameEmployeur = "N/A";
                    if(entreprise.getEmployeurPrincipal() != null){
                        usernameEmployeur = entreprise.getEmployeurPrincipal().getUsername();
                    }
                    return EmployeurListDto.builder()
                            .entrepriseId(entreprise.getId())
                            .nomEntreprise(entreprise.getNom())
                            .usernameEmployeur(usernameEmployeur)
                            .dateCreationEntreprise(entreprise.getDateCreation())
                            .build();
                })
                .collect(Collectors.toList());
    }


}
