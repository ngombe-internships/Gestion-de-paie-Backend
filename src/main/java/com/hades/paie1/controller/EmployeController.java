package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.dto.EmployeCreateDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.service.EmployeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employes")
@CrossOrigin(origins = "http://localhost:4200")
public class EmployeController {

    private  final  EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<EmployeResponseDto>> createEmploye(@Valid @RequestBody EmployeCreateDto employeCreateDto) {

            EmployeResponseDto createEmploye = employeService.createEmploye(employeCreateDto);
            ApiResponse<EmployeResponseDto> response = new ApiResponse<>("Emploie cree avec Succes", createEmploye, HttpStatus.CREATED);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR') or hasRole('EMPLOYE')")
    public  ResponseEntity<ApiResponse<List<EmployeResponseDto>>> searchEmploye(
            @RequestParam (required = false) String searchTerm) {

        List<EmployeResponseDto> employes = employeService.searchEmployes(searchTerm);
        ApiResponse<List<EmployeResponseDto>> response = new ApiResponse<>("Recherche d'employer effectuee avec succes", employes,HttpStatus.OK);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEUR','EMPLOYE')")
    public ResponseEntity<ApiResponse<EmployeResponseDto>> getEmployeById (@PathVariable Long id){
        Optional<EmployeResponseDto> employe = employeService.getEmployeById(id);
        if(employe.isPresent()){
            ApiResponse<EmployeResponseDto> response = new ApiResponse<>("Employe trouve", employe.get(), HttpStatus.OK);
            return ResponseEntity.ok(response);
        }
        else{
            ApiResponse<EmployeResponseDto> errorResponse = new ApiResponse<>("Employe non trouve avec ID: " +id, null, HttpStatus.NO_CONTENT);
             return  new ResponseEntity<>(errorResponse , HttpStatus.NOT_FOUND);
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<Void>> deleteEmploye(@PathVariable Long id){

            employeService.deleteEmploye(id);
        ApiResponse<Void> response = new ApiResponse<>("Employé supprimé avec succès", null, HttpStatus.NO_CONTENT);
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<EmployeResponseDto>> updateEmploye (@PathVariable Long id , @RequestBody EmployeCreateDto employeCreateDto) {
        EmployeResponseDto employe = employeService.updateEmploye( employeCreateDto, id);

        ApiResponse<EmployeResponseDto> response = new ApiResponse<>(
                "Employe mis a jour avec succces",
                employe,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEUR') ")
    public ResponseEntity<ApiResponse<List<EmployeResponseDto>>> getAllEmploye(){

        List<EmployeResponseDto> employe = employeService.getAllEmploye();
        ApiResponse<List<EmployeResponseDto>> response = new ApiResponse<>(
                "Liste de tous les Employes",
                employe,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response,HttpStatus.OK);
    }


    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<ApiResponse<EmployeResponseDto>> getMyEmployeProfile(){
        EmployeResponseDto employeResponseDto = employeService.getEmployeProfilByAuthenticateUser();
        ApiResponse<EmployeResponseDto> response = new ApiResponse<>(
                "Profil de l'employe connecte recupere avec succes",
                employeResponseDto,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @GetMapping("/count")
    @PreAuthorize("hasRole('EMPLOYEUR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getEmployeCountForEmployer(){
        long count = employeService.countEmployeForAuthenticatedEmployer();

        ApiResponse<Long> response = new ApiResponse<>(
                "Total des employes recuperer avec succes",
                count,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



}
