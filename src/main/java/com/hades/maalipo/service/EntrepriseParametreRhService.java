package com.hades.maalipo.service;

import com.hades.maalipo.dto.entreprise.EntrepriseParametreRhDto;
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
                // Constantes Heures
                buildParam(entreprise, "TAUX_HEURE_SUP1", PaieConstants.TAUX_HEURE_SUP1.toString(), "DECIMAL", "Taux heure supp 1"),
                buildParam(entreprise, "TAUX_HEURE_SUP2", PaieConstants.TAUX_HEURE_SUP2.toString(), "DECIMAL", "Taux heure supp 2"),
                buildParam(entreprise, "TAUX_HEURE_SUP3", PaieConstants.TAUX_HEURE_SUP3.toString(), "DECIMAL", "Taux heure supp 3"),
                buildParam(entreprise, "TAUX_HEURE_NUIT", PaieConstants.TAUX_HEURE_NUIT.toString(), "DECIMAL", "Taux heure nuit"),
                buildParam(entreprise, "TAUX_HEURE_FERIE", PaieConstants.TAUX_HEURE_FERIE.toString(), "DECIMAL", "Taux heure férié"),

                // Plafonds et seuils
                buildParam(entreprise, "PLAFOND_CNPS", PaieConstants.PLAFOND_CNPS.toString(), "DECIMAL", "Plafond CNPS"),
                buildParam(entreprise, "SEUIL_TAXE_COMMUNALE", PaieConstants.SEUIL_TAXE_COMMUNALE.toString(), "DECIMAL", "Seuil taxe communale"),

                // Taux de cotisations sociales - Part salarié
                buildParam(entreprise, "TAUX_CNPS_VIEILLESSE_SALARIE", PaieConstants.TAUX_CNPS_VIEILLESSE_SALARIE.toString(), "DECIMAL", "Taux CNPS vieillesse salarié"),
                buildParam(entreprise, "TAUX_CREDIT_FONCIER_SALARIE", PaieConstants.TAUX_CREDIT_FONCIER_SALARIE.toString(), "DECIMAL", "Taux crédit foncier salarié"),
                buildParam(entreprise, "TAUX_FNE_SALARIE", PaieConstants.TAUX_FNE_SALARIE.toString(), "DECIMAL", "Taux FNE salarié"),

                // Taux de cotisations sociales - Part patronale
                buildParam(entreprise, "TAUX_CNPS_VIEILLESSE_EMPLOYEUR", PaieConstants.TAUX_CNPS_VIEILLESSE_EMPLOYEUR.toString(), "DECIMAL", "Taux CNPS vieillesse employeur"),
                buildParam(entreprise, "TAUX_CNPS_ALLOCATIONS_FAMILIALES", PaieConstants.TAUX_CNPS_ALLOCATIONS_FAMILIALES.toString(), "DECIMAL", "Taux CNPS allocations familiales"),
                buildParam(entreprise, "TAUX_CNPS_ACCIDENTS_TRAVAIL", PaieConstants.TAUX_CNPS_ACCIDENTS_TRAVAIL.toString(), "DECIMAL", "Taux CNPS accidents travail"),
                buildParam(entreprise, "TAUX_CREDIT_FONCIER_PATRONAL", PaieConstants.TAUX_CREDIT_FONCIER_PATRONAL.toString(), "DECIMAL", "Taux crédit foncier patronal"),
                buildParam(entreprise, "TAUX_FNE_PATRONAL", PaieConstants.TAUX_FNE_PATRONAL.toString(), "DECIMAL", "Taux FNE patronal"),

                // Taux CAC
                buildParam(entreprise, "TAUX_CAC", PaieConstants.TAUX_CAC.toString(), "DECIMAL", "Taux CAC (Centimes Additionnels Communaux)"),

                // Congés
//                buildParam(entreprise, "JOUR_CONGER", PaieConstants.JOUR_CONGER.toString(), "DECIMAL", "Jour de congé"),
//                buildParam(entreprise, "JOURCONGESBASE", PaieConstants.JOURCONGESBASE.toString(), "DECIMAL", "Base jours congés"),

                // Primes d'ancienneté
                buildParam(entreprise, "TAUX_PRIME_ANCIENNETE_INIT", PaieConstants.TAUX_PRIME_ANCIENNETE_INIT.toString(), "DECIMAL", "Taux prime ancienneté initial"),
                buildParam(entreprise, "TAUX_PRIME_ANCIENNETE_SUPPL", PaieConstants.TAUX_PRIME_ANCIENNETE_SUPPL.toString(), "DECIMAL", "Taux prime ancienneté supplémentaire")
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
