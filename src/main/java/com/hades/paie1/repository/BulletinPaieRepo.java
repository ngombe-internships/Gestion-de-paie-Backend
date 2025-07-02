package com.hades.paie1.repository;

import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepo extends JpaRepository <BulletinPaie, Long> {

    List<BulletinPaie> findByEmploye (Employe employe);
    List<BulletinPaie> findByEmployeId(Long employeId);

    List<BulletinPaie> findByEntreprise(Entreprise entreprise);

//    List<BulletinPaie> findByEmployeAndStatusBulletin (Employe employe, List<StatusBulletin> status);
    List<BulletinPaie> findByEmployeAndStatusBulletinIn(Employe employe, List<StatusBulletin> statuses);

    List<BulletinPaie> findByEntrepriseOrderByDateCreationBulletinDesc(Entreprise entreprise);

    List<BulletinPaie> findByEntrepriseAndEmploye_NomContainingIgnoreCaseOrEmploye_PrenomContainingIgnoreCaseOrEmploye_MatriculeContainingIgnoreCaseOrderByDateCreationBulletinDesc(
            Entreprise entreprise, String nom, String prenom, String matricule);


    long countByEntreprise (Entreprise entreprise);
}
