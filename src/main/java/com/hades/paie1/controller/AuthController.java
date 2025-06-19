package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.dto.LoginDto;
import com.hades.paie1.dto.RegisterDto;
import com.hades.paie1.enum1.Role;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.UserRepository;
import com.hades.paie1.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private EmployeRepository employeRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;

    public AuthController (
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            EmployeRepository employeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.employeRepository = employeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    //Endpoint de connexion
    @PostMapping("login")
    public ResponseEntity<ApiResponse<String>> authenticateUser (@RequestBody LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        ApiResponse<String> response = new ApiResponse<>("Connexion reussie", token, HttpStatus.OK);
        return  new ResponseEntity<>(response, HttpStatus.OK);

    }
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody RegisterDto registerDto){

        if(userRepository.existsByUsername(registerDto.getUsername())) {
            return new ResponseEntity<>(new ApiResponse<>("Nom Utilisateur deja pris!", null, HttpStatus.BAD_REQUEST),HttpStatus.BAD_REQUEST);
        }

        Employe employe = null;
        if (registerDto.getEmployeId() != null) {
            Optional<Employe> existingEmploye = employeRepository.findById(registerDto.getEmployeId());
            if (existingEmploye.isEmpty()) {
                return new ResponseEntity<>(new ApiResponse<>("Employe non trouve avec ID: " + registerDto.getEmployeId(), null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
            }
            employe = existingEmploye.get();
            // Vérifier si cet employé a déjà un compte utilisateur
            if (userRepository.findByEmploye(employe).isPresent()) { // Ajoutez cette méthode à UserRepository
                return new ResponseEntity<>(new ApiResponse<>("Cet employe a deja un compte utilisateur.", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
            }
        }


        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(Role.EMPLOYE)
                .employe(employe)
                .build();

        userRepository.save(user);

        return new ResponseEntity<>(new ApiResponse<>("Utilisateur enregistre avec succes!", null, HttpStatus.CREATED), HttpStatus.CREATED);
    }
}
