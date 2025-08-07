package com.hades.maalipo.repository;

import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long>, JpaSpecificationExecutor<Entreprise> {
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

    @Query("""
    SELECT e FROM Entreprise e
    LEFT JOIN e.employeurPrincipal u
    WHERE (:nomEntreprise = '' OR LOWER(e.nom) LIKE LOWER(CONCAT('%', :nomEntreprise, '%')))
      AND (:usernameEmployeur = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :usernameEmployeur, '%')))
      AND (:status = '' OR e.active = CASE WHEN :status = 'active' THEN true ELSE false END)
  """)
    Page<Entreprise> findByFilters(
            @Param("nomEntreprise") String nomEntreprise,
            @Param("usernameEmployeur") String usernameEmployeur,
            @Param("status") String status,
            Pageable pageable
    );
}
