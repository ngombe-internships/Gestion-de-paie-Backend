package com.hades.paie1.repository;

import com.hades.paie1.dto.EmployeurListDto;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    Optional <Entreprise> findByNom (String nom);
    Optional <Entreprise> findByNumeroSiret (String numeroSiret);

    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.employeurPrincipal")
    List<Entreprise> findAllWithEmployeurPrincipalLoaded(Sort sort);
    Optional<Entreprise> findByEmployeurPrincipal(User employeurPrincipal);

    // Requête spécifique pour éviter les problèmes de LOB
    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.employeurPrincipal WHERE e.id = :id")
    Optional<Entreprise> findByIdWithEmployeur(@Param("id") Long id);

    // Pour la liste, éviter de charger les LOB

    Optional<Entreprise> findByEmailEntreprise(String emailEntreprise);

}
