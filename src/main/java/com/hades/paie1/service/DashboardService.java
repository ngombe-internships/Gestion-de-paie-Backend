package com.hades.paie1.service;

import com.hades.paie1.repository.BulletinPaieRepo;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.EntrepriseRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final EntrepriseRepository entrepriseRepository;
    private final EmployeRepository employeRepository;
    private final BulletinPaieRepo bulletinPaieRepo;

    public DashboardService(EntrepriseRepository entrepriseRepository,
                            EmployeRepository employeRepository,
                            BulletinPaieRepo bulletinPaieRepo) {
        this.entrepriseRepository = entrepriseRepository;
        this.employeRepository = employeRepository;
        this.bulletinPaieRepo = bulletinPaieRepo;
    }

    public long countEntreprises() {
        return entrepriseRepository.count();
    }

    public long countEmployes() {
        return employeRepository.count();
    }

    public long countBulletinsEmis(){
        return bulletinPaieRepo.count();
    }
}
