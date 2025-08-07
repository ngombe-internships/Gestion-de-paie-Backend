package com.hades.maalipo.repository;

import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional <User> findByUsername (String username);
    Boolean existsByUsername (String username);

    Optional <User> findByEmploye (Employe employe);

    Optional<User> findByRole (Role role);


}
