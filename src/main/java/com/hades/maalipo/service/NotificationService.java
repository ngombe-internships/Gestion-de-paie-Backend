package com.hades.maalipo.service;

import com.hades.maalipo.dto.notification.NotificationDto;
import com.hades.maalipo.model.Notification;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.NotificationRepository;
import com.hades.maalipo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // Pour WebSocket

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void creerNotification(Long userId, String titre, String message,
                                  Notification.TypeNotification type,
                                  Long referenceId, String referenceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);

        Notification saved = notificationRepository.save(notification);

        // Envoi en temps réel via WebSocket
        NotificationDto dto = convertToDto(saved);
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                dto
        );
    }

    public Page<NotificationDto> getNotificationsUtilisateur(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByDateCreationDesc(userId, pageable);
        return notifications.map(this::convertToDto);
    }

    public long countNotificationsNonLues(Long userId) {
        return notificationRepository.countByUserIdAndLuFalse(userId);
    }

    @Transactional
    public void marquerCommeLu(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        notification.setLu(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void marquerToutesCommeLues(Long userId) {
        notificationRepository.marquerToutesCommeLues(userId);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitre(notification.getTitre());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType().name());
        dto.setLu(notification.getLu());
        dto.setDateCreation(notification.getDateCreation());
        dto.setReferenceId(notification.getReferenceId());
        dto.setReferenceType(notification.getReferenceType());

        if ("DEMANDE_CONGE".equals(notification.getReferenceType())) {
            dto.setLienAction("/dashboard/conges/demandes/" + notification.getReferenceId());
        } else if ("BULLETIN_PAIE".equals(notification.getReferenceType())) {
            dto.setLienAction("/dashboard/bulletins/" + notification.getReferenceId());
        }
        return dto;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void nettoyerAnciennesNotifications() {
        try {
            LocalDateTime dateLimit = LocalDateTime.now().minusDays(30); // Garder 30 jours
            notificationRepository.supprimerNotificationsAnciennes(dateLimit);
            log.info("Nettoyage des notifications anciennes effectué");
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des notifications: {}", e.getMessage());
        }
    }

    @Transactional
    public void creerNotificationBulletinPaie(Long userId, String employeName, String periode,
                                              String montantNet, Long bulletinId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String titre = "📄 Nouveau bulletin de paie disponible";
        String message = String.format(
                "Bonjour %s,\n\n" +
                        "Votre bulletin de paie pour la période %s est maintenant disponible dans votre espace personnel.\n\n" +
                        "💰 Salaire net à payer : %s FCFA\n\n" +
                        "Vous pouvez dès maintenant le consulter, le télécharger et l'imprimer depuis votre tableau de bord.",
                employeName, periode, montantNet
        );

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(Notification.TypeNotification.BULLETIN_PAIE_DISPONIBLE);
        notification.setReferenceId(bulletinId);
        notification.setReferenceType("BULLETIN_PAIE");

        Notification saved = notificationRepository.save(notification);

        // Envoi en temps réel via WebSocket
        NotificationDto dto = convertToDto(saved);
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                dto
        );

        log.info("Notification bulletin de paie créée pour l'utilisateur {}: {}", user.getUsername(), titre);
    }
}