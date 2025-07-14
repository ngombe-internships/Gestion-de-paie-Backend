package com.hades.paie1.repository;

import com.hades.paie1.model.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface JourFerieRepository extends JpaRepository<JourFerie,Long> {

    Optional<JourFerie> findByDateFerie(LocalDate date);

    Optional<JourFerie> findByDateFerieAndEntrepriseId(LocalDate date , Long entrepriseId);

}
