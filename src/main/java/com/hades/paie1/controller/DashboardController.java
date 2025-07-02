package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController (DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getDashboardMetrics(){

        try{
            long entreprisesCount = dashboardService.countEntreprises();
            long employesCount = dashboardService.countEmployes();
            long bulletinsCount = dashboardService.countBulletinsEmis();

            Map<String, Long> metrics = new HashMap<>();
            metrics.put("nombreEntreprises", entreprisesCount);
            metrics.put("nombreEmploye", employesCount);
            metrics.put("nombreBulletins", bulletinsCount);

            ApiResponse<Map<String, Long>> response = new ApiResponse<>(
                    "Metrique du tableau de bord",
                    metrics,
                    HttpStatus.OK
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse<>(
                    "Erreur lors de la recuperation des metirque du tableau",
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR
            ),HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
