package com.hades.maalipo.repository;

import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.EmployePaieConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployePaieConfigRepository extends JpaRepository<EmployePaieConfig , Long>, JpaSpecificationExecutor<EmployePaieConfig> {

    List<EmployePaieConfig> findByEmploye(Employe employe);

    @Query("SELECT c FROM EmployePaieConfig c " +
            "WHERE c.employe = :employe " +
            "AND c.elementPaie.id = :elementPaieId " +
            "AND :periode >= c.dateDebut " +
            "AND (c.dateFin IS NULL OR :periode <= c.dateFin)")
    List<EmployePaieConfig> findActiveConfigForEmployeAndElementAndPeriode(
            @Param("employe") Employe employe,
            @Param("elementPaieId") Long elementPaieId,
            @Param("periode") LocalDate periode
    );

    List<EmployePaieConfig> findByEmployeIn(List<Employe> employes);


}
