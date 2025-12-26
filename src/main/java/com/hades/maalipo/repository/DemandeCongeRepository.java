package com.hades.maalipo.repository;

import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long>, JpaSpecificationExecutor<DemandeConge> {

    List<DemandeConge> findByEmploye(Employe employe);

    List<DemandeConge> findByEmployeAndStatutIn(Employe employe, List<StatutDemandeConge> statuts);

    List<DemandeConge> findByEmployeAndTypeConge(Employe employe, TypeConge typeConge);

    /**
     * Filtrage sur dateDemande (date de soumission) au lieu de dateDebut
     */
    @Query("SELECT d FROM DemandeConge d WHERE d.employe.id = :employeId " +
            "AND (:statut IS NULL OR d.statut = :statut) " +
            "AND (:year IS NULL OR EXTRACT(YEAR FROM d.dateDemande) = :year) " +  // ✅ Changé ici
            "AND (:searchTerm IS NULL OR : searchTerm = '' OR " +
            "LOWER(COALESCE(d. raison, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(COALESCE(d. motifRejet, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY d. dateDemande DESC")
    Page<DemandeConge> findDemandesFiltered(
            @Param("employeId") Long employeId,
            @Param("statut") StatutDemandeConge statut,
            @Param("year") Integer year,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

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

    @Query("SELECT d FROM DemandeConge d WHERE d.employe.entreprise.id = :entrepriseId " +
            "AND d.statut = :statut")
    List<DemandeConge> findByEntrepriseAndStatut(@Param("entrepriseId") Long entrepriseId,
                                                 @Param("statut") StatutDemandeConge statut);

    @Query("SELECT d FROM DemandeConge d WHERE d.statut = :statut AND " +
            "d.dateDemande <= :dateLimit")
    List<DemandeConge> findDemandesEnAttenteDepuisJours(@Param("statut") StatutDemandeConge statut,
                                                        @Param("dateLimit") LocalDate dateLimit);

    @Query("SELECT d FROM DemandeConge d WHERE d.dateFin = :date AND d.statut = :statut")
    List<DemandeConge> findByDateFinAndStatut(@Param("date") LocalDate date, @Param("statut") StatutDemandeConge statut);

    @Query("SELECT d FROM DemandeConge d WHERE d.employe.id = :employeId " +
            "AND d.dateDebut >= :dateDebut AND d.dateFin <= :dateFin " +
            "ORDER BY d.dateDebut DESC")
    List<DemandeConge> findByEmployeIdAndDateBetween(
            @Param("employeId") Long employeId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

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