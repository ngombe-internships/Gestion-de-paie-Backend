package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.DemandeCongeCreateDto;
import com.hades.maalipo.dto.conge.DemandeCongeResponseDto;
import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.reponse.PageResponse;
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.exception.CongeValidationException;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.mapper.DemandeCongeMapper;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.repository.UserRepository;
import com.hades.maalipo.service.AuditLogService;
import com.hades.maalipo.specification.DemandeCongeSpecifications;
import com.hades.maalipo.utils.JourOuvrableCalculatorUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DemandeCongeService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationCongeService.class);


    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final JourFerieService jourFerieService;
    private final UserRepository userRepository;
    private final CongeValidationService congeValidationService;
    private final SoldeCongeService soldeCongeService;
    private final NotificationCongeService notificationService;
    private final EntrepriseRepository entrepriseRepository;
    private final JourOuvrableCalculatorUtil jourOuvrableCalculator;
    private final AuditLogService auditLogService;


    public DemandeCongeService(DemandeCongeRepository demandeCongeRepository,
                               EmployeRepository employeRepository,
                               JourFerieService jourFerieService,
                               UserRepository userRepository,
                               CongeValidationService congeValidationService,
                               SoldeCongeService soldeCongeService,
                               NotificationCongeService notificationService,
                               EntrepriseRepository entrepriseRepository,
                               JourOuvrableCalculatorUtil jourOuvrableCalculator,
                               AuditLogService auditLogService) {

        this.demandeCongeRepository = demandeCongeRepository;
        this.employeRepository = employeRepository;
        this.jourFerieService = jourFerieService;
        this.userRepository = userRepository;
        this.congeValidationService = congeValidationService;
        this.soldeCongeService = soldeCongeService;
        this.notificationService = notificationService;
        this.entrepriseRepository = entrepriseRepository;
        this.jourOuvrableCalculator = jourOuvrableCalculator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DemandeCongeResponseDto submitDemandeConge(DemandeCongeCreateDto demandeCongeCreateDto, User currentUser) {
        Employe employe = employeRepository.findById(demandeCongeCreateDto.getEmployeId())
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + demandeCongeCreateDto.getEmployeId()));

        // 1. VALIDATION DES PERMISSIONS
        validateSubmissionPermissions(currentUser, employe);

        // 2. MISE À JOUR DU SOLDE POUR CONGÉS PAYÉS
        if (demandeCongeCreateDto.getTypeConge() == TypeConge.CONGE_PAYE) {
            BigDecimal soldeActuel = soldeCongeService.calculerSoldeAcquisTotal(employe);
            employe.setSoldeJoursConge(soldeActuel);
            employeRepository.save(employe);
        }

        // 3. VALIDATION MÉTIER COMPLÈTE
        CongeValidationService.ValidationResult validation =
                congeValidationService.validerDemandeConge(demandeCongeCreateDto, employe);

        if (!validation.isValide()) {
            throw new CongeValidationException(validation.getMessageErreurs());
        }

        // 4. CRÉATION ET SAUVEGARDE DE LA DEMANDE
        DemandeConge demande = DemandeCongeMapper.toEntity(demandeCongeCreateDto, employe);
        demande.setEmploye(employe);
        demande.setStatut(StatutDemandeConge.EN_ATTENTE);
        demande.setDateDemande(LocalDate.now());

        DemandeConge savedDemande = demandeCongeRepository.save(demande);

        auditLogService.logAction(
                "CREATE_DEMANDE_CONGE",
                "DemandeConge",
                savedDemande.getId(),
                currentUser.getUsername(),
                String.format("Demande de %s du %s au %s pour l'employé %s %s",
                        savedDemande.getTypeConge(),
                        savedDemande.getDateDebut(),
                        savedDemande.getDateFin(),
                        employe.getPrenom(),
                        employe.getNom())
        );

        try {
            // Envoi de notification de soumission (ne pas bloquer le workflow en cas d'erreur)
            notificationService.notifierSoumissionDemande(savedDemande);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de soumission: {}", e.getMessage(), e);
        }
        return DemandeCongeMapper.toResponseDto(savedDemande);
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


    private void validateSubmissionPermissions(User currentUser, Employe employe) {
        if (currentUser.getRole() == Role.EMPLOYE && !currentUser.getEmploye().getId().equals(employe.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à soumettre une demande de congé pour un autre employé.");
        }
        // TODO: Ajoutez d'autres vérifications si nécessaire
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
        auditLogService.logAction(
                "APPROVE_DEMANDE_CONGE",
                "DemandeConge",
                savedDemande.getId(),
                approuvePar.getUsername(),
                String.format("Demande approuvée: %s du %s au %s pour %s %s",
                        savedDemande.getTypeConge(),
                        savedDemande.getDateDebut(),
                        savedDemande.getDateFin(),
                        savedDemande.getEmploye().getPrenom(),
                        savedDemande.getEmploye().getNom())
        );

        try {
            // Notification d'approbation
            notificationService.notifierDecisionEmploye(savedDemande);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification d'approbation: {}", e.getMessage(), e);
        }
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

        auditLogService.logAction(
                "REJECT_DEMANDE_CONGE",
                "DemandeConge",
                savedDemande.getId(),
                approuveePar.getUsername(),
                String.format("Demande rejetée: %s du %s au %s pour %s %s. Motif: %s",
                        savedDemande.getTypeConge(),
                        savedDemande.getDateDebut(),
                        savedDemande.getDateFin(),
                        savedDemande.getEmploye().getPrenom(),
                        savedDemande.getEmploye().getNom(),
                        motifRejet)
        );

        try {
            // Notification de rejet
            notificationService.notifierDecisionEmploye(savedDemande);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de rejet: {}", e.getMessage(), e);
        }
        // Retourner un DTO de réponse
        return new DemandeCongeResponseDto(savedDemande);
    }

    @Transactional
    public DemandeCongeResponseDto cancelDemandeConge(Long demandeId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur non trouvé"));

        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Demande de congé non trouvée"));

        // Vérifier que l'utilisateur est autorisé à annuler cette demande
        boolean isAuthorized = false;

        // Si c'est un employé, vérifier qu'il est propriétaire de la demande
        if (user.getRole() == Role.EMPLOYE && user.getEmploye() != null) {
            isAuthorized = user.getEmploye().getId().equals(demande.getEmploye().getId());
        }
        // Si c'est un employeur, vérifier qu'il appartient à la même entreprise
        else if (user.getRole() == Role.EMPLOYEUR && user.getEntreprise() != null) {
            isAuthorized = user.getEntreprise().getId().equals(demande.getEmploye().getEntreprise().getId());
        }

        if (!isAuthorized) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à annuler cette demande de congé");
        }

        // Logique d'annulation...
        StatutDemandeConge oldStatut = demande.getStatut(); // Sauvegarder l'ancien statut
        demande.setStatut(StatutDemandeConge.ANNULEE);
        demande.setApprouveePar(user);
        demande.setDateApprobationRejet(LocalDate.now());

        DemandeConge savedDemande = demandeCongeRepository.save(demande);
        auditLogService.logAction(
                "CANCEL_DEMANDE_CONGE",
                "DemandeConge",
                savedDemande.getId(),
                user.getUsername(),
                String.format("Demande annulée: %s du %s au %s pour %s %s",
                        savedDemande.getTypeConge(),
                        savedDemande.getDateDebut(),
                        savedDemande.getDateFin(),
                        savedDemande.getEmploye().getPrenom(),
                        savedDemande.getEmploye().getNom())
        );

        // Si la demande était approuvée, restaurer le solde de congés
        if (oldStatut == StatutDemandeConge.APPROUVEE && demande.getTypeConge() == TypeConge.CONGE_PAYE) {
            Employe employe = demande.getEmploye();

            // Utiliser le nombre de jours ouvrables contractuels de l'employé
            int joursOuvrablesContractuels = employe.getJoursOuvrablesContractuelsHebdomadaires() != null
                    ? employe.getJoursOuvrablesContractuelsHebdomadaires()
                    : 5; // 5 jours par défaut si non spécifié

            long joursOuvrables = calculerJoursOuvrables(
                    demande.getDateDebut(),
                    demande.getDateFin(),
                    employe.getEntreprise().getId(),
                    joursOuvrablesContractuels);

            soldeCongeService.restaurerSoldeApresAnnulation(employe, joursOuvrables);
        }

        // Envoyer notifications...
        try {
            notificationService.notifierAnnulationConge(demande);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification d'annulation: {}", e.getMessage(), e);
        }

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
        return jourOuvrableCalculator.calculateWorkingDays(startDate, endDate, employe);
    }


    private long calculerJoursOuvrables(LocalDate dateDebut, LocalDate dateFin, Long entrepriseId, int joursOuvrablesHebdomadaires) {
        return jourOuvrableCalculator.calculateWorkingDays(dateDebut, dateFin, entrepriseId, joursOuvrablesHebdomadaires);
    }


    public PageResponse<DemandeCongeResponseDto> getDemandesCongeFiltered(
            Long employeId,
            User currentUser,
            String statut,
            Integer year,
            String searchTerm,
            int page,
            int size) {

        // Vérification des permissions
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + employeId));
        checkViewPermissionsForEmploye(currentUser, employe);

        // Configuration de la pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateDemande").descending());

        // Construction de la spécification
        Specification<DemandeConge> spec = Specification.where(DemandeCongeSpecifications.withEmployeId(employeId))
                .and(DemandeCongeSpecifications.withStatut(statut))
                .and(DemandeCongeSpecifications.withYear(year))
                .and(DemandeCongeSpecifications.withSearchTerm(searchTerm));

        // Exécution de la requête
        Page<DemandeConge> demandesPage = demandeCongeRepository.findAll(spec, pageable);

        // Conversion en DTOs
        List<DemandeCongeResponseDto> demandes = demandesPage.getContent().stream()
                .map(DemandeCongeResponseDto::new)
                .collect(Collectors.toList());

        // Création et retour du PageResponse
        return new PageResponse<>(
                demandes,
                demandesPage.getNumber(),
                demandesPage.getSize(),
                demandesPage.getTotalElements(),
                demandesPage.getTotalPages()
        );
    }




}