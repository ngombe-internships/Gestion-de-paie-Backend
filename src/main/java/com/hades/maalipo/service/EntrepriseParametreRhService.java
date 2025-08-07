package com.hades.maalipo.service;

import com.hades.maalipo.dto.EntrepriseParametreRhDto;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.EntrepriseParametreRh;
import com.hades.maalipo.repository.EntrepriseParametreRhRepository;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.utils.PaieConstants;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EntrepriseParametreRhService {
    private final EntrepriseParametreRhRepository paramRepo;
    private final EntrepriseRepository entrepriseRepo;

    public EntrepriseParametreRhService(EntrepriseParametreRhRepository paramRepo, EntrepriseRepository entrepriseRepo) {
        this.paramRepo = paramRepo;
        this.entrepriseRepo = entrepriseRepo;
    }

    public EntrepriseParametreRhDto toDto(EntrepriseParametreRh entity){
        return EntrepriseParametreRhDto.builder()
                .id(entity.getId())
                .entrepriseId(entity.getEntreprise().getId())
                .cleParametre(entity.getCleParametre())
                .valeurParametre(entity.getValeurParametre())
                .description(entity.getDescription())
                .build();
    }

    public EntrepriseParametreRh toEntity(EntrepriseParametreRhDto dto) {
       return EntrepriseParametreRh.builder()
                .id(dto.getId())
                .entreprise(entrepriseRepo.findById(dto.getEntrepriseId()).orElse(null))
                .cleParametre(dto.getCleParametre())
                .valeurParametre(dto.getValeurParametre())
                .description(dto.getDescription())
                .build();
    }

    //Crud
    public List<EntrepriseParametreRhDto> getAllParamsForEntreprise(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepo.findById(entrepriseId).orElseThrow();
        return paramRepo.findByEntreprise(entreprise)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<EntrepriseParametreRhDto> getParam(Long entrepriseId, String cleParametre) {
        Entreprise entreprise = entrepriseRepo.findById(entrepriseId).orElseThrow();
        return paramRepo.findByEntrepriseAndCleParametre(entreprise, cleParametre)
                .map(this::toDto);
    }



    @Transactional
    public EntrepriseParametreRhDto updateParam(Long id, EntrepriseParametreRhDto dto) {
        EntrepriseParametreRh entity = paramRepo.findById(id).orElseThrow();
        entity.setValeurParametre(dto.getValeurParametre());
        entity.setDescription(dto.getDescription());
        return toDto(paramRepo.save(entity));
    }



    // Initialisation des paramètres RH par défaut
    @Transactional
    public void initDefaultParamsForEntreprise(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepo.findById(entrepriseId).orElseThrow();
        List<EntrepriseParametreRh> defaultParams = List.of(
                buildParam(entreprise, "TAUX_HEURE_SUP1", PaieConstants.TAUX_HEURE_SUP1.toString(), "DECIMAL", "Taux heure supp 1"),
                buildParam(entreprise, "TAUX_HEURE_SUP2", PaieConstants.TAUX_HEURE_SUP2.toString(), "DECIMAL", "Taux heure supp 2"),
                buildParam(entreprise, "TAUX_HEURE_SUP3", PaieConstants.TAUX_HEURE_SUP3.toString(), "DECIMAL", "Taux heure supp 3"),
                buildParam(entreprise, "TAUX_HEURE_NUIT", PaieConstants.TAUX_HEURE_NUIT.toString(), "DECIMAL", "Taux heure nuit"),
                buildParam(entreprise, "TAUX_HEURE_FERIE", PaieConstants.TAUX_HEURE_FERIE.toString(), "DECIMAL", "Taux heure férié"),
                buildParam(entreprise, "PLAFOND_CNPS", PaieConstants.PLAFOND_CNPS.toString(), "DECIMAL", "Plafond CNPS"),
                buildParam(entreprise, "SEUIL_TAXE_COMMUNALE", PaieConstants.SEUIL_TAXE_COMMUNALE.toString(), "DECIMAL", "Seuil taxe communale"),
                buildParam(entreprise, "JOUR_CONGER", PaieConstants.JOUR_CONGER.toString(), "DECIMAL", "Jour de congé"),
                buildParam(entreprise, "JOURCONGESBASE", PaieConstants.JOURCONGESBASE.toString(), "DECIMAL", "Base jours congés"),
                buildParam(entreprise, "TAUX_PRIME_ANCIENNETE_INIT", PaieConstants.TAUX_PRIME_ANCIENNETE_INIT.toString(), "DECIMAL", "Taux prime ancienneté initial"),
                buildParam(entreprise, "TAUX_PRIME_ANCIENNETE_SUPPL", PaieConstants.TAUX_PRIME_ANCIENNETE_SUPPL.toString(), "DECIMAL", "Taux prime ancienneté suppl.")
        );
        paramRepo.saveAll(defaultParams);
    }

    private EntrepriseParametreRh buildParam(Entreprise entreprise, String cle, String valeur, String type, String desc) {
        return EntrepriseParametreRh.builder()
                .entreprise(entreprise)
                .cleParametre(cle)
                .valeurParametre(valeur)
                .typeParametre(type)
                .description(desc)
                .build();
    }

    public String getParamOrDefault(Long entrepriseId, String cle, String defaultValue) {
        return getParam(entrepriseId, cle)
                .map(EntrepriseParametreRhDto::getValeurParametre)
                .orElse(defaultValue);
    }
}
