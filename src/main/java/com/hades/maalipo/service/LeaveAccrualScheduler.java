package com.hades.maalipo.service;

import com.hades.maalipo.model.Employe;
import com.hades.maalipo.repository.EmployeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeaveAccrualScheduler {
    private EmployeService employeService;
    private EmployeRepository employeRepo;

    public LeaveAccrualScheduler (EmployeService employeService, EmployeRepository employeRepo){
        this.employeRepo= employeRepo;
        this.employeService= employeService;
    }

    @Scheduled(cron = "0 0 0 1 * ?") //ceci est pour chaque 1 ere du mois a 00h00min00s
    //@Scheduled(fixedRate = 60000)
    public void runMonthyLeaveAccrual(){

        System.out.println("Debut de l'execution de la tache planifiee de mise a jour des conges...");

        List<Employe> allEmployes = employeRepo.findAll();

        for(Employe employe : allEmployes){
            employeService.updateEmployeLeaveBalance(employe);
        }
        System.out.println("Fin de l'execution de la tache planifiee de mise a jour des conges.");

    }
}
