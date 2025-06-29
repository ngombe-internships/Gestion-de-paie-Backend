package com.hades.paie1.service;// Dans un service, par exemple AuthService.java
// Ajoutez cette annotation pour gérer la transaction
import com.hades.paie1.dto.RegisterDto;
import com.hades.paie1.enum1.Role;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hades.paie1.repository.EmployeRepository; // Ajoutez l'import

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeRepository employeRepository; // Ajoutez cette ligne
    private final PasswordEncoder passwordEncoder;

    // Constructeur pour l'injection des dépendances
    public AuthService(UserRepository userRepository, EmployeRepository employeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeRepository = employeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional // <--- C'est la clé !
    public void registerEmployeeAccount(RegisterDto registerDto) {
        // ... (vos validations ici)

        Employe employe = employeRepository.findById(registerDto.getEmployeId())
                .orElseThrow(() -> new RessourceNotFoundException("Employe non trouve avec ID: " + registerDto.getEmployeId()));

        // Les vérifications supplémentaires...
        if (userRepository.findByEmploye(employe).isPresent()) {
            throw new IllegalStateException("Cet employe a deja un compte utilisateur.");
        }

        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(Role.EMPLOYE)
                .employe(employe)
                .build();

        employe.setUser(user);

        // Sauvegardez les deux entités dans la même transaction
        userRepository.save(user); // Ou employeRepository.save(employe);
    }
}