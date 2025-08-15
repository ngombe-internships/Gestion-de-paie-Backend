package com.hades.maalipo.repository;

import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional <User> findByUsername (String username);
    Boolean existsByUsername (String username);

    Optional <User> findByEmploye (Employe employe);

    Optional<User> findByRole (Role role);

    @Query("SELECT u FROM User u WHERE u.entreprise.id = :entrepriseId AND u.role = 'EMPLOYEUR' ORDER BY u.id")
    User findFirstEmployeurByEntrepriseId(@Param("entrepriseId") Long entrepriseId);


}
