package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.JourFerieDto;
import com.hades.maalipo.dto.conge.JourFerieRequestDto;
import com.hades.maalipo.dto.conge.JourFerieUpdateDto;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.JourFerie;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.repository.JourFerieRepository;
import com.hades.maalipo.service.AuditLogService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JourFerieService {

    private final JourFerieRepository jourFerieRepo;
    private final EntrepriseRepository entrepriseRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public JourFerieService(JourFerieRepository jourFerieRepo,
                            EntrepriseRepository entrepriseRepository,
                            AuditLogService auditLogService) {
        this.jourFerieRepo = jourFerieRepo;
        this.entrepriseRepository = entrepriseRepository;
        this.auditLogService = auditLogService;

    }

    @Transactional
    public JourFerie addJourFerie(JourFerieRequestDto jourFerieDto) {
        // Vérification de la date
        if (jourFerieRepo.findByDateFerie(jourFerieDto.getDateFerie()).isPresent()) {
            throw new IllegalStateException("Un jour férié existe déjà à cette date: " + jourFerieDto.getDateFerie());
        }

        // Récupération de l'entreprise
        Entreprise entreprise = entrepriseRepository.findById(jourFerieDto.getEntrepriseId())
                .orElseThrow(() -> new RessourceNotFoundException("Entreprise non trouvée avec l'ID: " + jourFerieDto.getEntrepriseId()));

        // Création du jour férié
        JourFerie jourFerie = new JourFerie();
        jourFerie.setNom(jourFerieDto.getNom());
        jourFerie.setDateFerie(jourFerieDto.getDateFerie());
        jourFerie.setEstChomeEtPaye(jourFerieDto.getEstChomeEtPaye());
        jourFerie.setEntreprise(entreprise);

        JourFerie savedJourFerie = jourFerieRepo.save(jourFerie);

        auditLogService.logAction(
                "CREATE_JOUR_FERIE",
                "JourFerie",
                savedJourFerie.getId(),
                auditLogService.getCurrentUsername(),
                String.format("Jour férié créé: %s le %s pour l'entreprise %d",
                        savedJourFerie.getNom(),
                        savedJourFerie.getDateFerie(),
                        entreprise.getId())
        );

        return savedJourFerie;
    }

    // Conservez vos méthodes existantes...
    @Transactional
    public JourFerie addJourFerieEntity(JourFerie jourFerie) {
        if (jourFerieRepo.findByDateFerie(jourFerie.getDateFerie()).isPresent()) {
            throw new IllegalStateException("Un jour férié existe déjà à cette date: " + jourFerie.getDateFerie());
        }
        return jourFerieRepo.save(jourFerie);
    }

    public Optional<JourFerie> getJourFerieById(Long id) {
        return jourFerieRepo.findById(id);
    }

    public List<JourFerieDto> getAllJoursFeriesDto() {
        return jourFerieRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private JourFerieDto convertToDto(JourFerie jourFerie) {
        JourFerieDto dto = new JourFerieDto();
        dto.setId(jourFerie.getId());
        dto.setDateFerie(jourFerie.getDateFerie());
        dto.setNom(jourFerie.getNom());
        dto.setEstChomeEtPaye(jourFerie.getEstChomeEtPaye());

        if (jourFerie.getEntreprise() != null) {
            dto.setEntrepriseId(jourFerie.getEntreprise().getId());
            dto.setEntrepriseNom(jourFerie.getEntreprise().getNom());
        }

        return dto;
    }

    @Transactional
    public JourFerieUpdateDto updateJourFerie(Long id, JourFerie jourFerieDetails) {
        JourFerie jourFerie = jourFerieRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Jour férié non trouvé avec l'ID: " + id));

        jourFerie.setDateFerie(jourFerieDetails.getDateFerie());
        jourFerie.setNom(jourFerieDetails.getNom());
        jourFerie.setEstChomeEtPaye(jourFerieDetails.getEstChomeEtPaye());

        // Si l'entreprise est fournie dans les détails, la mettre à jour
        if (jourFerieDetails.getEntreprise() != null) {
            jourFerie.setEntreprise(jourFerieDetails.getEntreprise());
        }

        JourFerie saved =  jourFerieRepo.save(jourFerie);

        auditLogService.logAction(
                "UPDATE_JOUR_FERIE",
                "JourFerie",
                saved.getId(),
                auditLogService.getCurrentUsername(),
                String.format("Jour férié modifié: %s le %s",
                        saved.getNom(),
                        saved.getDateFerie())
        );

        JourFerieUpdateDto dto = new JourFerieUpdateDto();
        dto.setId(saved.getId());
        dto.setDateFerie(saved.getDateFerie());
        dto.setNom(saved.getNom());
        dto.setEstChomeEtPaye(saved.getEstChomeEtPaye());
        dto.setEntrepriseId(saved.getEntreprise() != null ? saved.getEntreprise().getId() : null);

        return dto;
    }

    @Transactional
    public void deleteId(Long id) {
        JourFerie jourFerie = jourFerieRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Jour férié non trouvé avec l'ID: " + id));

        String details = String.format("Jour férié supprimé: %s le %s",
                jourFerie.getNom(), jourFerie.getDateFerie());

        jourFerieRepo.deleteById(id);

        auditLogService.logAction(
                "DELETE_JOUR_FERIE",
                "JourFerie",
                id,
                auditLogService.getCurrentUsername(),
                details
        );
    }

    public boolean isJourFerie(LocalDate date, Long entrepriseId) {
        return jourFerieRepo.findByDateFerieAndEntrepriseId(date, entrepriseId).isPresent();
    }

    public Optional<JourFerie> getJoutFerieByDate(LocalDate date) {
        return jourFerieRepo.findByDateFerie(date);
    }
}