package com.hades.maalipo.repository;

import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.EntrepriseParametreRh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseParametreRhRepository extends JpaRepository<EntrepriseParametreRh, Long> {
    List<EntrepriseParametreRh> findByEntreprise(Entreprise entreprise);
    Optional<EntrepriseParametreRh> findByEntrepriseAndCleParametre(Entreprise entreprise, String cleParametre);
}
