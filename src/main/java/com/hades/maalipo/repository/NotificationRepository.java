package com.hades.maalipo.repository;

import com.hades.maalipo.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Trouve les notifications d'un utilisateur triées par date de création décroissante
     */
    Page<Notification> findByUserIdOrderByDateCreationDesc(Long userId, Pageable pageable);

    /**
     * Compte les notifications non lues d'un utilisateur
     */
    long countByUserIdAndLuFalse(Long userId);

    /**
     * Trouve une notification par ID et utilisateur (sécurité)
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.user.id = :userId AND n.lu = false")
    void marquerToutesCommeLues(@Param("userId") Long userId);

    /**
     * Supprime les anciennes notifications (nettoyage automatique)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.dateCreation < :dateLimit")
    void supprimerNotificationsAnciennes(@Param("dateLimit") LocalDateTime dateLimit);

    /**
     * Trouve les notifications non lues récentes d'un utilisateur
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.lu = false ORDER BY n.dateCreation DESC")
    List<Notification> findNotificationsNonLuesRecentes(@Param("userId") Long userId, Pageable pageable);

    /**
     * Trouve les notifications par type pour un utilisateur
     */
    List<Notification> findByUserIdAndType(Long userId, Notification.TypeNotification type);

    /**
     * Compte toutes les notifications d'un utilisateur
     */
    long countByUserId(Long userId);
}