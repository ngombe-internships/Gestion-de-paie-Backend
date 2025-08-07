package com.hades.maalipo.controller;

import com.hades.maalipo.dto.AvantageNatureDto;
import com.hades.maalipo.model.EmployeAvantageNature;
import com.hades.maalipo.service.EmployeAvantageNatureService;
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
