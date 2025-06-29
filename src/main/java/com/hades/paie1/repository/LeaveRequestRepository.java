package com.hades.paie1.repository;

import com.hades.paie1.enum1.StatutConge;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository  extends JpaRepository <LeaveRequest, Long> {

    @Query("SELECT d FROM LeaveRequest d WHERE d.employe = :employe " +
            "AND d.statut IN :statuts " +
            "AND ((d.dateDebutSouhaitee <= :dateFinMois AND d.dateFinSouhaitee >= :dateDebutMois))")
    List<LeaveRequest> findDemandesCongeChevauchantes(
            @Param("employe") Employe employe,
            @Param("statuts") List<StatutConge> statuts,
            @Param("dateDebutMois") LocalDate dateDebutMois,
            @Param("dateFinMois") LocalDate dateFinMois);

    // Recherche les congés terminés dans une période de paie
    @Query("SELECT d FROM LeaveRequest d WHERE d.employe = :employe " +
            "AND d.statut = 'TERMINE' " +
            "AND d.dateRetourEffectif BETWEEN :dateDebutMois AND :dateFinMois")
    List<LeaveRequest> findCongesTerminesForPaiePeriod(
            @Param("employe") Employe employe,
            @Param("dateDebutMois") LocalDate dateDebutMois,
            @Param("dateFinMois") LocalDate dateFinMois);
}
