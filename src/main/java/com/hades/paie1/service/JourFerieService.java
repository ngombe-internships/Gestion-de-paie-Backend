package com.hades.paie1.service;

import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.JourFerie;
import com.hades.paie1.repository.JourFerieRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JourFerieService {

    public JourFerieRepository jourFerieRepo;

    public JourFerieService(JourFerieRepository jourFerieRepo) {
        this.jourFerieRepo = jourFerieRepo;
    }

    @Transactional
    public JourFerie addJourFerie(JourFerie jourFerie){

        if(jourFerieRepo.findByDateFerie(jourFerie.getDateFerie()).isPresent()) {
            throw new IllegalStateException("Un jour ferie existe deja  a cette date: " +jourFerie.getDateFerie());
        }
        return jourFerieRepo.save(jourFerie);
    }

    public Optional<JourFerie> getJourFerieById(Long id){
        return jourFerieRepo.findById(id);
    }

    public List<JourFerie> getAllJoursFeries(){
        return jourFerieRepo.findAll();
    }

    @Transactional
    public JourFerie updateJourFerie(Long id, JourFerie jourFerieDetails){
        JourFerie jourFerie = jourFerieRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Jour ferier non trouve avec l'Id :"+ id ));

        jourFerie.setDateFerie(jourFerieDetails.getDateFerie());
        jourFerie.setNom(jourFerie.getNom());
        jourFerie.setEstChomeEtPaye(jourFerieDetails.getEstChomeEtPaye());

        return jourFerieRepo.save(jourFerie);
    }

    @Transactional
    public  void  deleteId(Long id){
        if(!jourFerieRepo.existsById(id)){
            throw new RessourceNotFoundException("Jour ferie non trouve avec l'ID: " +id);
        }
        jourFerieRepo.deleteById(id);
    }

    public boolean isJourFerie(LocalDate date, Long entrepriseId){


        return jourFerieRepo.findByDateFerieAndEntrepriseId(date, entrepriseId).isPresent();
    }

    public Optional<JourFerie>getJoutFerieByDate(LocalDate date) {
        return jourFerieRepo.findByDateFerie(date);
    }




}
