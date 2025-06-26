package com.hades.paie1.repository;

import com.hades.paie1.enum1.Role;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional <User> findByUsername (String username);
    Boolean existsByUsername (String username);

    Optional <User> findByEmploye (Employe employe);

    Optional<User> findByRole (Role role);
}
