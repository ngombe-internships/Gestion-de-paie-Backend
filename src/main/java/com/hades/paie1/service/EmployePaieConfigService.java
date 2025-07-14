package com.hades.paie1.service;

import com.hades.paie1.dto.ElementPaieDto;
import com.hades.paie1.dto.EmployePaieConfigDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.EmployePaieConfig;
import com.hades.paie1.repository.ElementPaieRepository;
import com.hades.paie1.repository.EmployePaieConfigRepository;
import com.hades.paie1.repository.EmployeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployePaieConfigService {

    private final EmployePaieConfigRepository employePaieConfigRepository;
    private final EmployeRepository employeRepository;
    private final ElementPaieRepository elementPaieRepository;

    public EmployePaieConfigService(EmployePaieConfigRepository employePaieConfigRepository,
                                    EmployeRepository employeRepository,
                                    ElementPaieRepository elementPaieRepository) {
        this.employePaieConfigRepository = employePaieConfigRepository;
        this.employeRepository = employeRepository;
        this.elementPaieRepository = elementPaieRepository;
    }


    private EmployePaieConfigDto convertToDto(EmployePaieConfig entity) {
        EmployePaieConfigDto dto = new EmployePaieConfigDto();
        dto.setId(entity.getId());
        dto.setTaux(entity.getTaux());
        dto.setMontant(entity.getMontant());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());

        if (entity.getEmploye() != null) {
            EmployeResponseDto employeDto = new EmployeResponseDto();
            Employe employeEntity = entity.getEmploye();
            employeDto.setId(employeEntity.getId());
            employeDto.setNom(employeEntity.getNom());
            employeDto.setPrenom(employeEntity.getPrenom());
            employeDto.setMatricule(employeEntity.getMatricule());
            // Ajouter d'autres champs si nécessaire
            dto.setEmploye(employeEntity.getId());
        }

        if (entity.getElementPaie() != null) {
            ElementPaieDto elementPaieDto = new ElementPaieDto();
            ElementPaie elementPaieEntity = entity.getElementPaie();
            elementPaieDto.setId(elementPaieEntity.getId());
            elementPaieDto.setCode(elementPaieEntity.getCode());
            elementPaieDto.setType(elementPaieEntity.getType());
            elementPaieDto.setFormuleCalcul(elementPaieEntity.getFormuleCalcul());
            elementPaieDto.setTauxDefaut(elementPaieEntity.getTauxDefaut());
//            elementPaieDto.setUniteBaseCalcul(elementPaieEntity.getUniteBaseCalcul());
            elementPaieDto.setCategorie(elementPaieEntity.getCategorie());
            elementPaieDto.setDesignation(elementPaieEntity.getDesignation());
            elementPaieDto.setImpacteSalaireBrut(elementPaieEntity.isImpacteSalaireBrut());
            elementPaieDto.setImpacteBaseCnps(elementPaieEntity.isImpacteBaseCnps());
            // Ajouter d'autres champs si nécessaire
            dto.setElementPaie(elementPaieDto.getId());
        }
        return dto;
    }
    public List<EmployePaieConfigDto> getAllEmployePaieConfigs() {
        List<EmployePaieConfig> configs = employePaieConfigRepository.findAll();

        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<EmployePaieConfigDto> getEmployePaieConfigById(Long id) {
        return employePaieConfigRepository.findById(id)
                .map(this::convertToDto); // Convertir l'entité en DTO avant de retourner
    }

    @Transactional
    public List<EmployePaieConfigDto> getEmployePaieConfigsByEmployeId(Long employeId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employe non trouvé avec id : " + employeId));
        List<EmployePaieConfig> configs = employePaieConfigRepository.findByEmploye(employe);
        // Optionnel: Forcer l'initialisation des relations LAZY.
        // for (EmployePaieConfig config : configs) {
        //     config.getElementPaie().getIntitule();
        // }
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    @Transactional
    public EmployePaieConfigDto createEmployePaieConfig(Long employeId, Long elementPaieId, EmployePaieConfig newConfig) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employe non trouvé avec id : " + employeId));
        ElementPaie elementPaie = elementPaieRepository.findById(elementPaieId)
                .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id : " + elementPaieId));

        newConfig.setEmploye(employe);
        newConfig.setElementPaie(elementPaie);
        newConfig.setDateDebut(newConfig.getDateDebut() != null ? newConfig.getDateDebut() : LocalDate.now());

        EmployePaieConfig createdConfig = employePaieConfigRepository.save(newConfig);
        return convertToDto(createdConfig); // Convertir en DTO avant de retourner
    }

    @Transactional
    public EmployePaieConfigDto updateEmployePaieConfig(Long id, EmployePaieConfig updatedConfig) {
        EmployePaieConfig existingConfig = employePaieConfigRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("EmployePaieConfig non trouvé avec id : " + id));

        existingConfig.setValeur(updatedConfig.getValeur());
        existingConfig.setDateDebut(updatedConfig.getDateDebut());
        existingConfig.setDateFin(updatedConfig.getDateFin());

        EmployePaieConfig savedConfig = employePaieConfigRepository.save(existingConfig);
        return convertToDto(savedConfig); // Convertir en DTO avant de retourner
    }

    public void deleteEmployePaieConfig(Long id) {
        if (!employePaieConfigRepository.existsById(id)) {
            throw new RessourceNotFoundException("EmployePaieConfig non trouvé avec id : " + id);
        }
        employePaieConfigRepository.deleteById(id);
    }
}
