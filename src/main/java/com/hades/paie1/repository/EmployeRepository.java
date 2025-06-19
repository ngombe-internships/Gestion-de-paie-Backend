package com.hades.paie1.repository;

import com.hades.paie1.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe , Long > , JpaSpecificationExecutor<Employe> {


    boolean existsByMatricule (String matricule);

    boolean existsByNumeroCnps(String nCnps);
    boolean existsByNiu (String niu);

    boolean existsByEmail (String email);

    Optional<Employe> findByMatricule(String matricule);
}
