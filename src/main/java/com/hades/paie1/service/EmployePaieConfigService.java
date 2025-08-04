package com.hades.paie1.service;

import com.hades.paie1.dto.ElementPaieDto;
import com.hades.paie1.dto.EmployePaieConfigDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.enum1.Role;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.*;
import com.hades.paie1.repository.ElementPaieRepository;
import com.hades.paie1.repository.EmployePaieConfigRepository;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public EmployePaieConfigService(EmployePaieConfigRepository employePaieConfigRepository,
                                    EmployeRepository employeRepository,
                                    ElementPaieRepository elementPaieRepository,
                                    AuditLogService auditLogService,
                                    UserRepository userRepository) {
        this.employePaieConfigRepository = employePaieConfigRepository;
        this.employeRepository = employeRepository;
        this.elementPaieRepository = elementPaieRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }


    private EmployePaieConfigDto convertToDto(EmployePaieConfig entity) {
        EmployePaieConfigDto dto = new EmployePaieConfigDto();
        dto.setId(entity.getId());
        dto.setTaux(entity.getTaux());
        dto.setMontant(entity.getMontant());
        dto.setNombre(entity.getNombre());
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
            elementPaieDto.setMontantDefaut(elementPaieEntity.getMontantDefaut());
            elementPaieDto.setNombreDefaut(elementPaieEntity.getNombreDefaut());
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

    public List<EmployePaieConfigDto> getEmployePaieConfigsForAuthenticatedEmployer() {
        // 1. Récupère l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 2. Vérifie qu'il est EMPLOYEUR
        if (user.getRole() != Role.EMPLOYEUR) {
            throw new AccessDeniedException("Seul un employeur peut voir les configs de ses employés.");
        }

        // 3. Récupère son entreprise
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new IllegalStateException("Employeur sans entreprise.");
        }

        // 4. Récupère tous les employés de cette entreprise
        List<Employe> employes = employeRepository.findByEntreprise(entreprise);

        // 5. Récupère toutes les configs liées à ces employés
        List<EmployePaieConfig> configs = employePaieConfigRepository.findByEmployeIn(employes);

        // 6. Convertit en DTO
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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

        auditLogService.logAction(
                "CREATE_EMPLOYE_PAIE_CONFIG",
                "EmployePaieConfig",
                createdConfig.getId(),
                auditLogService.getCurrentUsername(),
                "Création d'une configuration de paie pour l'employé id=" + employe.getNom() + "" + employe.getPrenom() + employe.getEntreprise().getNom()
        );

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
        auditLogService.logAction(
                "UPDATE_EMPLOYE_PAIE_CONFIG",
                "EmployePaieConfig",
                savedConfig.getId(),
                auditLogService.getCurrentUsername(),
                "Mise à jour d'une configuration de paie pour l'employé id=" + savedConfig.getEmploye().getId()
        );
        return convertToDto(savedConfig); // Convertir en DTO avant de retourner
    }

    public void deleteEmployePaieConfig(Long id) {
        if (!employePaieConfigRepository.existsById(id)) {
            throw new RessourceNotFoundException("EmployePaieConfig non trouvé avec id : " + id);
        }
        employePaieConfigRepository.deleteById(id);
        auditLogService.logAction(
                "DELETE_EMPLOYE_PAIE_CONFIG",
                "EmployePaieConfig",
                id,
                auditLogService.getCurrentUsername(),
                "Suppression d'une configuration de paie employé id=" + id
        );
    }

    public Page<EmployePaieConfigDto> searchConfigsForAuthenticatedEmployer(
            Long employeId,
            Long elementPaieId,
            String status, // "active" ou "all"
            String searchTerm,
            Pageable pageable
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (user.getRole() != Role.EMPLOYEUR) {
            throw new AccessDeniedException("Seul un employeur peut voir les configs de ses employés.");
        }
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new IllegalStateException("Employeur sans entreprise.");
        }

        Specification<EmployePaieConfig> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            // Filtre sur les employés de l'entreprise
            var employeJoin = root.join("employe");
            predicates = cb.and(predicates, cb.equal(employeJoin.get("entreprise"), entreprise));

            // Filtre employé
            if (employeId != null) {
                predicates = cb.and(predicates, cb.equal(employeJoin.get("id"), employeId));
            }

            // Filtre élément paie
            if (elementPaieId != null) {
                var elementJoin = root.join("elementPaie");
                predicates = cb.and(predicates, cb.equal(elementJoin.get("id"), elementPaieId));
            }

            // Filtre statut
            if ("active".equalsIgnoreCase(status)) {
                var today = LocalDate.now();
                predicates = cb.and(
                        predicates,
                        cb.lessThanOrEqualTo(root.get("dateDebut"), today),
                        cb.or(
                                cb.isNull(root.get("dateFin")),
                                cb.greaterThanOrEqualTo(root.get("dateFin"), today)
                        )
                );
            }

            // Filtre recherche
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String likeTerm = "%" + searchTerm.trim().toLowerCase() + "%";
                predicates = cb.and(predicates,
                        cb.or(
                                cb.like(cb.lower(employeJoin.get("nom")), likeTerm),
                                cb.like(cb.lower(employeJoin.get("prenom")), likeTerm),
                                cb.like(cb.lower(employeJoin.get("matricule")), likeTerm)
                                // tu peux ajouter d'autres champs ici
                        )
                );
            }

            return predicates;
        };

        Page<EmployePaieConfig> page = employePaieConfigRepository.findAll(spec, pageable);
        return page.map(this::convertToDto);
    }
}
