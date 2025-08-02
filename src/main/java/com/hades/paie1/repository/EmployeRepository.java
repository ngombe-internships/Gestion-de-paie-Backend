package com.hades.paie1.repository;

import com.hades.paie1.model.Employe;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe , Long > , JpaSpecificationExecutor<Employe> {


    boolean existsByMatricule (String matricule);

    boolean existsByNumeroCnps(String nCnps);
    boolean  existsByNiu (String niu);

    boolean existsByEmail (String email);

    Optional<Employe> findByMatricule(String matricule);

    List<Employe> findByEntreprise(Entreprise entreprise);

    Optional <Employe> findByUser (User user);

    long countByEntreprise (Entreprise entreprise);

    Optional<Employe> findByEmail(String email);

    @Query("SELECT e.id FROM Employe e WHERE e.entreprise.id = :entrepriseId")
    List<Long> findIdsByEntrepriseId(@Param("entrepriseId") Long entrepriseId);


    Page<Employe> findByEntrepriseOrderById(Entreprise entreprise, Pageable pageable);

}
