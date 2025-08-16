package com.hades.maalipo.controller;

import com.hades.maalipo.dto.notification.NotificationDto;
import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.model.Notification;
import com.hades.maalipo.model.User;
import com.hades.maalipo.service.EmployeService;
import com.hades.maalipo.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeService employeService;

    public NotificationController(NotificationService notificationService,
                                  EmployeService employeService) {
        this.notificationService = notificationService;
        this.employeService = employeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getMesNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User currentUser = employeService.getAuthenticatedUser();
        Page<NotificationDto> notifications = notificationService
                .getNotificationsUtilisateur(currentUser.getId(), PageRequest.of(page, size));

        return ResponseEntity.ok(new ApiResponse<>(
                "Notifications récupérées avec succès",
                notifications,
                HttpStatus.OK));
    }

    @GetMapping("/count-non-lues")
    public ResponseEntity<ApiResponse<Long>> getCountNotificationsNonLues() {
        User currentUser = employeService.getAuthenticatedUser();
        long count = notificationService.countNotificationsNonLues(currentUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Nombre de notifications non lues",
                count,
                HttpStatus.OK));
    }

    @PutMapping("/{id}/marquer-lu")
    public ResponseEntity<ApiResponse<Void>> marquerCommeLu(@PathVariable Long id) {
        User currentUser = employeService.getAuthenticatedUser();
        notificationService.marquerCommeLu(id, currentUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Notification marquée comme lue",
                null,
                HttpStatus.OK));
    }

    @PutMapping("/marquer-toutes-lues")
    public ResponseEntity<ApiResponse<Void>> marquerToutesCommeLues() {
        User currentUser = employeService.getAuthenticatedUser();
        notificationService.marquerToutesCommeLues(currentUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "Toutes les notifications marquées comme lues",
                null,
                HttpStatus.OK));
    }


}