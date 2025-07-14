package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.dto.AvantageNatureDto;
import com.hades.paie1.model.EmployeAvantageNature;
import com.hades.paie1.service.EmployeAvantageNatureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avantage")
public class EmployeAvantageController {

    private final EmployeAvantageNatureService service;

    public EmployeAvantageController (EmployeAvantageNatureService service){
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmployeAvantageNature> addAvantage(@RequestBody AvantageNatureDto dto){
        EmployeAvantageNature avantage = service.createAvantage(dto);
        return ResponseEntity.ok(avantage);
    }

//    @PutMapping
//    public ResponseEntity<EmployeAvantageNature>
}
