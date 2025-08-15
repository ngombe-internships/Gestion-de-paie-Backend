package com.hades.maalipo.repository;

import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long >, JpaSpecificationExecutor {

    List<DemandeConge> findByEmploye(Employe employe);

    List<DemandeConge> findByEmployeAndStatutIn(Employe employe, List<StatutDemandeConge> statuts);

    List<DemandeConge> findByEmployeAndTypeConge(Employe employe, TypeConge typeConge);

    /**
     * Trouver les demandes par employé, type et période
     */
    @Query("SELECT dc FROM DemandeConge dc WHERE dc.employe = :employe " +
            "AND dc.typeConge = :typeConge " +
            "AND dc.dateDebut BETWEEN :dateDebut AND :dateFin")
    List<DemandeConge> findByEmployeAndTypeCongeAndDateDebutBetween(
            @Param("employe") Employe employe,
            @Param("typeConge") TypeConge typeConge,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Vérifier les chevauchements pour un employé sur une période
     */
    @Query("SELECT dc FROM DemandeConge dc WHERE dc.employe = :employe " +
            "AND dc.statut IN :statuts " +
            "AND NOT (dc.dateFin < :dateDebut OR dc.dateDebut > :dateFin)")
    List<DemandeConge> findOverlappingDemandesForEmploye(
            @Param("employe") Employe employe,
            @Param("statuts") List<StatutDemandeConge> statuts,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Compter les demandes par type pour un employé dans l'année
     */
    @Query("SELECT COUNT(dc) FROM DemandeConge dc WHERE dc.employe = :employe " +
            "AND dc.typeConge = :typeConge " +
            "AND YEAR(dc.dateDebut) = YEAR(:annee)")
    long countByEmployeAndTypeCongeInYear(
            @Param("employe") Employe employe,
            @Param("typeConge") TypeConge typeConge,
            @Param("annee") LocalDate annee
    );

    @Query("SELECT dc FROM DemandeConge dc WHERE dc.employe.id = :employeId AND dc.typeConge = com.hades.maalipo.enum1.TypeConge.CONGE_MARIAGE AND dc.statut = com.hades.maalipo.enum1.StatutDemandeConge.APPROUVEE")
    Optional<DemandeConge> findMariageLeave(@Param("employeId") Long employeId);

    @Query("SELECT d FROM DemandeConge d WHERE d.employe.entreprise.id = :entrepriseId " +
            "AND d.statut IN (:statut1, :statut2) " +
            "AND ((d.dateDebut BETWEEN :dateDebut AND :dateFin) " +
            "OR (d.dateFin BETWEEN :dateDebut AND :dateFin) " +
            "OR (:dateDebut BETWEEN d.dateDebut AND d.dateFin))")
    List<DemandeConge> findByEntrepriseAndDateRange(@Param("entrepriseId") Long entrepriseId,
                                                    @Param("dateDebut") LocalDate dateDebut,
                                                    @Param("dateFin") LocalDate dateFin,
                                                    @Param("statut1") StatutDemandeConge statut1,
                                                    @Param("statut2") StatutDemandeConge statut2);

    // Dans DemandeCongeRepository.java, ajouter ces méthodes:

    @Query("SELECT d FROM DemandeConge d WHERE d.employe.entreprise.id = :entrepriseId " +
            "AND d.statut = :statut")
    List<DemandeConge> findByEntrepriseAndStatut(@Param("entrepriseId") Long entrepriseId,
                                                 @Param("statut") StatutDemandeConge statut);


    @Query("SELECT d FROM DemandeConge d WHERE d.statut = :statut AND " +
            "DATEDIFF(CURRENT_DATE, d.dateDemande) >= :joursAttente")
    List<DemandeConge> findDemandesEnAttenteDepuisJours(@Param("statut") StatutDemandeConge statut,
                                                        @Param("joursAttente") int joursAttente);


    @Query("SELECT d FROM DemandeConge d WHERE d.dateFin = :date AND d.statut = :statut")
    List<DemandeConge> findByDateFinAndStatut(@Param("date") LocalDate date, @Param("statut") StatutDemandeConge statut);


    // NOUVELLE REQUÊTE : Trouver les demandes par employé, statut et période
    @Query("SELECT d FROM DemandeConge d WHERE d.employe.id = :employeId " +
            "AND d.dateDebut >= :dateDebut AND d.dateFin <= :dateFin " +
            "ORDER BY d.dateDebut DESC")
    List<DemandeConge> findByEmployeIdAndDateBetween(
            @Param("employeId") Long employeId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );


    //NOUVELLE REQUÊTE : Pour l'historique des congés
    @Query("SELECT d FROM DemandeConge d WHERE d.employe = :employe " +
            "AND d.statut = :statut " +
            "AND d.dateDebut >= :dateDebut AND d.dateDebut <= :dateFin")
    List<DemandeConge> findByEmployeAndStatutAndDateBetween(
            @Param("employe") Employe employe,
            @Param("statut") StatutDemandeConge statut,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );


}

