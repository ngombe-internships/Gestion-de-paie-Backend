package com.hades.maalipo.service;

import com.hades.maalipo.dto.DemandeCongeCreateDto;
import com.hades.maalipo.dto.DemandeCongeResponseDto;
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final JourFerieService jourFerieService;
    private final UserRepository userRepository;

    public DemandeCongeService(DemandeCongeRepository demandeCongeRepository,
                               EmployeRepository employeRepository,
                               JourFerieService jourFerieService,
                               UserRepository userRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.employeRepository = employeRepository;
        this.jourFerieService = jourFerieService;
        this.userRepository = userRepository;
    }

    @Transactional
    public DemandeCongeResponseDto submitDemandeConge(DemandeCongeCreateDto demandeCongeCreateDto, User currentUser) {
        Employe employe = employeRepository.findById(demandeCongeCreateDto.getEmployeId())
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + demandeCongeCreateDto.getEmployeId()));

        DemandeConge demandeConge = new DemandeConge();
        demandeConge.setEmploye(employe);
        demandeConge.setDateDebut(demandeCongeCreateDto.getDateDebut());
        demandeConge.setDateFin(demandeCongeCreateDto.getDateFin());
        demandeConge.setTypeConge(demandeCongeCreateDto.getTypeConge());
        demandeConge.setRaison(demandeCongeCreateDto.getRaison());

        // Définir les valeurs par défaut ou gérées par le serveur
        demandeConge.setStatut(StatutDemandeConge.EN_ATTENTE);
        demandeConge.setDateDemande(LocalDate.now());

        DemandeConge savedDemande = demandeCongeRepository.save(demandeConge);

        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(savedDemande);
    }

    public List<DemandeCongeResponseDto> getDemandesCongeByEmploye(Long employeId, User currentUser) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + employeId));

        checkViewPermissionsForEmploye(currentUser, employe);

        List<DemandeConge> demandes = demandeCongeRepository.findByEmploye(employe);

        // Convertir la liste d'entités en liste de DTOs
        return demandes.stream()
                .map(DemandeCongeResponseDto::new)
                .collect(Collectors.toList());
    }

    public DemandeCongeResponseDto getDemandeCongeById(Long demandeId, User currentUser) {
        DemandeConge demandeConge = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande de congé non trouvée avec l'ID: " + demandeId));

        Employe targetEmploye = demandeConge.getEmploye();
        if (targetEmploye == null) {
            throw new IllegalStateException("La demande de congé n'est pas associée à un employé");
        }

        checkViewPermissionsForEmploye(currentUser, targetEmploye);

        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(demandeConge);
    }

    @Transactional
    public DemandeCongeResponseDto approveDemandeConge(Long demandeId, Long approuveeParUserId) {
        DemandeConge demandeConge = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande de congé non trouvée avec l'ID: " + demandeId));

        // Vérifier le statut de la demande
        if (demandeConge.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new IllegalArgumentException("La demande de congé n'est pas en attente d'approbation.");
        }

        User approuvePar = userRepository.findById(approuveeParUserId)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur d'approbation non trouvé avec l'ID: " + approuveeParUserId));

        // Vérification des autorisations
        checkActionPermissions(approuvePar, demandeConge);

        // Vérifier si l'utilisateur est employeur
        if (!(approuvePar.getRole() == Role.EMPLOYEUR)) {
            throw new SecurityException("L'utilisateur n'a pas les permissions requises pour approuver les congés.");
        }

        demandeConge.setStatut(StatutDemandeConge.APPROUVEE);
        demandeConge.setDateApprobationRejet(LocalDate.now());
        demandeConge.setApprouveePar(approuvePar);

        // Décompter les jours
        Employe employe = demandeConge.getEmploye();
        long joursOuvrablesApprouves = calculateWorkingDays(
                demandeConge.getDateDebut(),
                demandeConge.getDateFin(),
                employe
        );

        if (demandeConge.getTypeConge() == TypeConge.CONGE_PAYE) {
            if (employe.getSoldeJoursConge() == null || employe.getSoldeJoursConge().compareTo(BigDecimal.valueOf(joursOuvrablesApprouves)) < 0) {
                throw new IllegalStateException("Erreur interne: Solde de congés insuffisant lors de l'approbation. Solde: " +
                        (employe.getSoldeJoursConge() != null ? employe.getSoldeJoursConge() : BigDecimal.ZERO) +
                        ", Jours à déduire: " + joursOuvrablesApprouves);
            }
            employe.setSoldeJoursConge(employe.getSoldeJoursConge().subtract(BigDecimal.valueOf(joursOuvrablesApprouves)));
            employeRepository.save(employe);
        }

        DemandeConge savedDemande = demandeCongeRepository.save(demandeConge);

        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(savedDemande);
    }

    @Transactional
    public DemandeCongeResponseDto rejectDemandeConge(Long demandeId, Long approuveeParUserId, String motifRejet) {
        DemandeConge demandeConge = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande de congé non trouvée avec l'ID: " + demandeId));

        if (demandeConge.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new IllegalArgumentException("La demande de congé n'est pas en attente de rejet.");
        }

        User approuveePar = userRepository.findById(approuveeParUserId)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur de rejet non trouvé avec l'ID: " + approuveeParUserId));

        // Vérification des autorisations
        checkActionPermissions(approuveePar, demandeConge);

        // Vérifier si l'utilisateur a le rôle employeur
        if (!(approuveePar.getRole() == Role.EMPLOYEUR)) {
            throw new SecurityException("L'utilisateur n'a pas les permissions requises pour rejeter les congés.");
        }

        demandeConge.setStatut(StatutDemandeConge.REJETEE);
        demandeConge.setDateApprobationRejet(LocalDate.now());
        demandeConge.setApprouveePar(approuveePar);
        demandeConge.setMotifRejet(motifRejet);

        DemandeConge savedDemande = demandeCongeRepository.save(demandeConge);

        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(savedDemande);
    }

    @Transactional
    public DemandeCongeResponseDto cancelDemandeConge(Long demandeId, Long annuleParUserId) {
        DemandeConge demandeConge = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande de congé non trouvée avec l'ID: " + demandeId));

        User annulePar = userRepository.findById(annuleParUserId)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur d'annulation non trouvé avec l'ID: " + annuleParUserId));

        // Vérifier si la demande peut être annulée
        if (demandeConge.getStatut() == StatutDemandeConge.REJETEE || demandeConge.getStatut() == StatutDemandeConge.ANNULEE) {
            throw new IllegalArgumentException("La demande de congé a déjà été rejetée ou annulée.");
        }

        // Vérifier les permissions
        checkActionPermissions(annulePar, demandeConge);

        if (annulePar.getRole() == Role.EMPLOYE) {
            if (!annulePar.getEmploye().getId().equals(demandeConge.getEmploye().getId())) {
                throw new SecurityException("Un employé ne peut annuler que ses propres demandes de congé.");
            }
        } else if (annulePar.getRole() != Role.EMPLOYEUR) {
            throw new SecurityException("L'utilisateur n'a pas les permissions requises pour annuler les congés.");
        }

        if (demandeConge.getStatut() == StatutDemandeConge.APPROUVEE && demandeConge.getTypeConge() == TypeConge.CONGE_PAYE) {
            Employe employe = demandeConge.getEmploye();
            long joursOuvrablesAnnules = calculateWorkingDays(
                    demandeConge.getDateDebut(),
                    demandeConge.getDateFin(),
                    employe
            );
            employe.setSoldeJoursConge(employe.getSoldeJoursConge().add(BigDecimal.valueOf(joursOuvrablesAnnules)));
            employeRepository.save(employe);
        }

        demandeConge.setStatut(StatutDemandeConge.ANNULEE);
        demandeConge.setDateApprobationRejet(LocalDate.now());
        demandeConge.setApprouveePar(annulePar);

        DemandeConge savedDemande = demandeCongeRepository.save(demandeConge);

        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(savedDemande);
    }

    private void checkActionPermissions(User actingUser, DemandeConge demandeConge) {
        Employe employeDemande = demandeConge.getEmploye();

        if (employeDemande == null || employeDemande.getEntreprise() == null) {
            throw new IllegalStateException("L'employé associé à la demande ou son entreprise n'est pas renseigné.");
        }
        if (actingUser.getEntreprise() == null) {
            throw new IllegalArgumentException("L'utilisateur agissant n'est pas associé à une entreprise.");
        }

        // Vérifier si l'utilisateur appartient à l'entreprise
        if (!actingUser.getEntreprise().getId().equals(employeDemande.getEntreprise().getId())) {
            throw new SecurityException("L'utilisateur n'est pas autorisé à agir sur des demandes pour cette entreprise.");
        }
    }

    private void checkViewPermissionsForEmploye(User currentUser, Employe targetEmploye) {
        if (targetEmploye == null || targetEmploye.getEntreprise() == null) {
            throw new IllegalStateException("L'employé cible ou son entreprise n'est pas renseigné.");
        }

        if (currentUser.getRole() == Role.EMPLOYE) {
            // Un employé ne peut voir que ses propres demandes
            if (currentUser.getEmploye() == null || !currentUser.getEmploye().getId().equals(targetEmploye.getId())) {
                throw new AccessDeniedException("Un employé ne peut voir que ses propres demandes de congé.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            // Un employeur peut voir les demandes des employés de sa propre entreprise
            if (currentUser.getEntreprise() == null || !currentUser.getEntreprise().getId().equals(targetEmploye.getEntreprise().getId())) {
                throw new AccessDeniedException("L'employeur n'est pas autorisé à voir les demandes de cette entreprise.");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            // Seuls les ADMINs ont un accès universel
            throw new AccessDeniedException("Accès refusé. L'utilisateur n'a pas les permissions requises pour voir ces demandes.");
        }
    }

    private long calculateWorkingDays(LocalDate startDate, LocalDate endDate, Employe employe) {
        long workingDays = 0;
        LocalDate current = startDate;

        int joursOuvrablesContractuelsHebdomadaires = employe.getJoursOuvrablesContractuelsHebdomadaires();

        while (!current.isAfter(endDate)) {
            // Vérifier si le jour actuel est un jour férié
            boolean isJourFerie = jourFerieService.isJourFerie(current, employe.getEntreprise().getId());

            // Vérifier si le jour actuel est un jour ouvrable pour l'employé
            boolean isWorkingDayBasedOnContract = true;

            // Logique pour exclure les week-ends selon le régime de l'employé
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (joursOuvrablesContractuelsHebdomadaires == 5) { // Si l'employé travaille 5 jours/semaine (L-V)
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    isWorkingDayBasedOnContract = false;
                }
            } else if (joursOuvrablesContractuelsHebdomadaires == 6) { // Si l'employé travaille 6 jours/semaine (L-S)
                if (dayOfWeek == DayOfWeek.SUNDAY) {
                    isWorkingDayBasedOnContract = false;
                }
            }
            // Ajoutez d'autres régimes si nécessaire (ex: 4 jours/semaine, etc.)

            // Un jour est compté comme ouvrable si ce n'est PAS un jour férié ET si c'est un jour ouvrable selon le contrat
            if (!isJourFerie && isWorkingDayBasedOnContract) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }
}