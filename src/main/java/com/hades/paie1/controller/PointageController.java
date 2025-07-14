//package com.hades.paie1.controller;
//
//
//import com.hades.paie1.dto.ApiResponse;
//import com.hades.paie1.exception.RessourceNotFoundException;
//import com.hades.paie1.model.Pointage;
//import com.hades.paie1.service.PointageService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.lang.module.ResolutionException;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/pointages")
//public class PointageController {
//
//    private PointageService pointageService;
//
//    public PointageController (PointageService pointageService){
//        this.pointageService = pointageService;
//    }
//
//    //pour enregistrer un nouveau pointage
//    @PostMapping
//    public ResponseEntity<ApiResponse<Pointage>> createPointage (@RequestBody Pointage pointage ){
//
//            Pointage newPointage = pointageService.savePointage(pointage);
//
//            ApiResponse<Pointage> response = new ApiResponse<>(
//                    "Creation du pointeur realise avec succes",
//                    newPointage,
//                    HttpStatus.CREATED
//            );
//            return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }
//
//    //Pour recupere tous les evenement
//    @GetMapping("/employe/{employeId}")
//    public ResponseEntity<ApiResponse<List<Pointage>>> getPointagesForEmploye(
//            @PathVariable Long employeId,
//            @RequestParam("startDate") LocalDate startDate,
//            @RequestParam("endDate") LocalDate endDate) {
//
//            List<Pointage> pointages = pointageService.getPointagesByEmployeAndDate(employeId, startDate, endDate);
//       ApiResponse<List<Pointage>> response = new ApiResponse<>(
//               "recupuretation  de l'employe avec ID realiser avec succes: " +employeId,
//               pointages,
//               HttpStatus.OK
//       );
//       return new ResponseEntity<>(response, HttpStatus.OK);
//    }
//
//    //pour déclencher le calcul des heures catégorisées
//
//    @GetMapping("/calcul-heures/employe/{employeId}")
//    public ResponseEntity<ApiResponse<Map<String, Double>>> calculateEmployeeHours(
//            @PathVariable Long employeId,
//            @RequestParam("startDate") LocalDate startDate,
//            @RequestParam("endDate") LocalDate endDate) {
//
//            Map<String, Double> categorizedHours = pointageService.calculateCategorizedHours(employeId, startDate, endDate);
//
//            ApiResponse<Map<String, Double>> response = new ApiResponse<>(
//                    "le calcul des heures de employe",
//                    categorizedHours,
//                    HttpStatus.OK
//            );
//        return new ResponseEntity<>(response,HttpStatus.OK );
//
//    }
//
//}
//
