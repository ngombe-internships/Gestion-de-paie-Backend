package com.hades.paie1.config;

import com.hades.paie1.enum1.Role;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // Logique de création de l'ADMIN existante (ne pas la modifier)
        if (userRepository.findByRole(Role.ADMIN).isEmpty()) {
            System.out.println("Aucun utilisateur ADMIN trouve. Creation de l'utilisteur ADMIN par defaut...");
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("adminpass"))
                    .role(Role.ADMIN)
                    .employe(null)
                    .build();
            userRepository.save(adminUser);
            System.out.println("Utilisateur ADMIN 'admin' cree avec succes !");
        } else {
            System.out.println("Un utilisateur ADMIN existe deja. Pas de creation d'utilisateur par defaut. ");
        }

//        // --- POUR LA CRÉATION DE L'EMPLOYEUR (pour les tests) ---
//        if (userRepository.findByRole(Role.EMPLOYEUR).isEmpty()) {
//            System.out.println("Aucun utilisateur EMPLOYEUR trouve. Creation de l'utilisteur EMPLOYEUR par defaut...");
//            User employeurUser = User.builder()
//                    .username("employeur") // Nom d'utilisateur pour le compte EMPLOYEUR
//                    .password(passwordEncoder.encode("employeurpass")) // Mot de passe
//                    .role(Role.EMPLOYEUR) // Assigne le rôle EMPLOYEUR
//                    .employe(null) // Laissez null si le champ 'employe' est nullable dans User
//                    .build();
//            userRepository.save(employeurUser);
//            System.out.println("Utilisateur EMPLOYEUR 'employeur' cree avec succes !");
//        } else {
//            System.out.println("Un utilisateur EMPLOYEUR existe deja. Pas de creation d'utilisateur par defaut.");
//        }

    }
}