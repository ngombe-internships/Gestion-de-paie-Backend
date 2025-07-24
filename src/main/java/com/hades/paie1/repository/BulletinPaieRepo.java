package com.hades.paie1.repository;

import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepo extends JpaRepository <BulletinPaie, Long> {

    List<BulletinPaie> findByEmploye (Employe employe);


//    List<BulletinPaie> findByEmployeAndStatusBulletin (Employe employe, List<StatusBulletin> status);
    List<BulletinPaie> findByEmployeAndStatusBulletinIn(Employe employe, List<StatusBulletin> statuses);

    List<BulletinPaie> findByEntrepriseOrderByDateCreationBulletinDesc(Entreprise entreprise);

    List<BulletinPaie> findByEntrepriseAndEmploye_NomContainingIgnoreCaseOrEmploye_PrenomContainingIgnoreCaseOrEmploye_MatriculeContainingIgnoreCaseOrderByDateCreationBulletinDesc(
            Entreprise entreprise, String nom, String prenom, String matricule);

    @Query("SELECT b FROM BulletinPaie b " +
            "LEFT JOIN FETCH b.lignesPaie l " +
            "LEFT JOIN FETCH l.elementPaie " +
            "LEFT JOIN FETCH b.employe " +
            "LEFT JOIN FETCH b.entreprise " +
            "WHERE b.id = :id")
    Optional<BulletinPaie> findByIdWithEverything(@Param("id") Long id);

    long countByEntreprise (Entreprise entreprise);
}
