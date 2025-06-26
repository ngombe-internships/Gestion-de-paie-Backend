package com.hades.paie1.repository;

import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    Optional <Entreprise> findByNom (String nom);
    Optional <Entreprise> findByNumeroSiret (String numeroSiret);

    Optional<Entreprise> findByEmployeurPrincipal(User employeurPrincipal);

}
