package com.hades.paie1.service.calculators;

import com.hades.paie1.model.Employe;
import com.hades.paie1.repository.EmployeRepository;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Service
public class AncienneteService {


    private EmployeRepository employeRepository;
    public  AncienneteService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }


    public int calculAncienneteEnAnnees (LocalDate dateEmbauche){
        if (dateEmbauche == null)
            return 0;
        return Period.between(dateEmbauche, LocalDate.now()).getYears();
    }

    public int calculAncienneteEnMois (LocalDate dateEmbauche) {
        if (dateEmbauche == null)
            return 0;
        Period period = Period.between(dateEmbauche, LocalDate.now());
        return period.getYears() * 12 + period.getMonths();
    }


    public Optional<Integer> getAncienneteEnAnneEmploye (Long employeId) {
        return  employeRepository.findById(employeId)
                .map(Employe::getDateEmbauche)
                .map(this::calculAncienneteEnAnnees);
    }
}
