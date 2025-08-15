package com.hades.maalipo.repository;

import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.SoldeHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SoldeHistoriqueRepository extends JpaRepository<SoldeHistorique, Long> {
    Optional<SoldeHistorique> findTopByEmployeOrderByIdDesc(Employe employe);
}