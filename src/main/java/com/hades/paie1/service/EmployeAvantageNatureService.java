package com.hades.paie1.service;

import com.hades.paie1.dto.AvantageNatureDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.EmployeAvantageNature;
import com.hades.paie1.model.EmployePaieConfig;
import com.hades.paie1.repository.EmployeNatureRepo;
import com.hades.paie1.repository.EmployeRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeAvantageNatureService {
    private final EmployeRepository employeRepo;
    private final EmployeNatureRepo repo;

    public EmployeAvantageNatureService (EmployeNatureRepo repo, EmployeRepository employeRepo ){
        this.employeRepo = employeRepo;
        this.repo = repo;
    }

    public EmployeAvantageNature createAvantage (AvantageNatureDto dto){
        Employe employe = employeRepo.findById(dto.getEmployeId())
                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouvé avec id " +dto.getEmployeId()));

        EmployeAvantageNature av = new EmployeAvantageNature();
        av.setEmploye(employe);
        av.setTypeAvantage(dto.getTypeAvantage());
        av.setActif(dto.getActif() != null ? dto.getActif() : true);
        return repo.save(av);
    }

    public EmployeAvantageNature updateAvantage(Long id, AvantageNatureDto dto ){
        EmployeAvantageNature existing = repo.findById(id)
                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouvé avec id " +id));
         existing.setId(dto.getId());
         existing.setTypeAvantage(dto.getTypeAvantage());
         existing.setActif(dto.getActif());

         return repo.save(existing);
    }
}
