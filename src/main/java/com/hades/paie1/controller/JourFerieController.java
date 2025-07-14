package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.JourFerie;
import com.hades.paie1.service.JourFerieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jours-feries")
public class JourFerieController {

    private final JourFerieService jourferieService;

    private JourFerieController(JourFerieService jourferieService){
        this.jourferieService = jourferieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<JourFerie>> getAllJourFeries(@PathVariable Long id){
        Optional <JourFerie> jour = jourferieService.getJourFerieById(id);

        if(jour.isPresent()){
            ApiResponse<JourFerie> response = new ApiResponse<>(
                    "Jour ferie trouve",
                    jour.get(),
                    HttpStatus.OK
            );
            return new ResponseEntity<>(response,HttpStatus.OK);
        } else{
            ApiResponse<JourFerie> errorResponse = new ApiResponse<>(
                    "Jour ferie non trouve avec ID: " +id,
                    null,
                    HttpStatus.NO_CONTENT
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NO_CONTENT);
        }

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<List<JourFerie>>> getJourFeriesByid(){
        List <JourFerie> newjour = jourferieService.getAllJoursFeries();

        ApiResponse<List<JourFerie>> response = new ApiResponse<>(
                "Jour ferie ajouter avec succes",
                newjour,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response,HttpStatus.OK);
    }



    @PostMapping
    public ResponseEntity<ApiResponse<JourFerie>> addJourFerie(@RequestBody JourFerie jourFerie ){

        JourFerie newjour = jourferieService.addJourFerie(jourFerie);

        ApiResponse<JourFerie> response = new ApiResponse<>(
                "Jour ferie ajouter avec succes",
                newjour,
                HttpStatus.CREATED
        );
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }


    @PutMapping
    public ResponseEntity<ApiResponse<JourFerie>> updateJourFerie(@PathVariable Long id, @RequestBody JourFerie jourFerie ){

        JourFerie newjour = jourferieService.updateJourFerie(id,jourFerie);

        ApiResponse<JourFerie> response = new ApiResponse<>(
                "Mise a jour du Jour ferie effectuer avec succes",
                newjour,
                HttpStatus.NO_CONTENT
        );
        return new ResponseEntity<>(response,HttpStatus.NO_CONTENT);
    }


    @GetMapping("/date/{date}")
    public ResponseEntity<JourFerie> getJourFerieByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date); //
        JourFerie jourFerie = jourferieService.getJoutFerieByDate(localDate)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun jour férié trouvé pour la date : " + date));
        return ResponseEntity.ok(jourFerie);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJourFerie(@PathVariable Long id) {
        jourferieService.deleteId(id);

        ApiResponse<Void> response = new ApiResponse<>(
                "Jour Ferier supprime avec succes",
                null,
                HttpStatus.NO_CONTENT
        );
        return new ResponseEntity<>(response,HttpStatus.NO_CONTENT); // 204 No Content
    }
}
