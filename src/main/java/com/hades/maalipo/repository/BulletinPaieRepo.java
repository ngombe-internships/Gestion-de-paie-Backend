package com.hades.maalipo.repository;

import com.hades.maalipo.enum1.StatusBulletin;
import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.Entreprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT b FROM BulletinPaie b " +
            "LEFT JOIN FETCH b.lignesPaie l " +
            "LEFT JOIN FETCH l.elementPaie " +
            "LEFT JOIN FETCH b.employe " +
            "LEFT JOIN FETCH b.entreprise " +
            "WHERE b.id = :id")
    Optional<BulletinPaie> findByIdWithEverything(@Param("id") Long id);
    long countByEntreprise (Entreprise entreprise);

    Page<BulletinPaie> findByEntrepriseOrderByDateCreationBulletinDesc(Entreprise entreprise, Pageable pageable);

    Page<BulletinPaie> findByEntrepriseAndStatusBulletinInOrderByDateCreationBulletinDesc(
            Entreprise entreprise, List<StatusBulletin> statuts, Pageable pageable
    );

    Page<BulletinPaie> findByEntrepriseAndStatusBulletinInAndEmploye_NomContainingIgnoreCaseOrEmploye_PrenomContainingIgnoreCaseOrEmploye_MatriculeContainingIgnoreCaseOrderByDateCreationBulletinDesc(
            Entreprise entreprise, List<StatusBulletin> statuts, String nom, String prenom, String matricule, Pageable pageable
    );
}
