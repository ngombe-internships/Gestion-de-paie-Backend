package com.hades.paie1.controller;

import com.hades.paie1.dto.*;
import com.hades.paie1.enum1.Role;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.EntrepriseRepository;
import com.hades.paie1.repository.UserRepository;
import com.hades.paie1.security.JwtTokenProvider;
import com.hades.paie1.service.AuthService;
import com.hades.paie1.service.FileStorageService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private EmployeRepository employeRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private EntrepriseRepository entrepriseRepository;
    private FileStorageService fileStorageService;
    private AuthService authService;

    public AuthController (
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            EmployeRepository employeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EntrepriseRepository entrepriseRepository,
            FileStorageService fileStorageService,
            AuthService authService
            ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.employeRepository = employeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.entrepriseRepository = entrepriseRepository;
        this.fileStorageService = fileStorageService;
        this.authService = authService;
    }

    //Endpoint de connexion
    @PostMapping("login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> authenticateUser (@RequestBody LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(()-> new RuntimeException("Utilisateur non trouve"));

        //recuperer le nom d'utilisateur et le role de l'objet

        String role = user.getRole().name();
        String displayName = user.getUsername();
        Long entrepriseId = null;

        //Logique pour determiner
        if(user.getRole() == Role.EMPLOYE) {
            Optional<Employe> employeOptional = employeRepository.findByUser(user);
            if (employeOptional.isPresent()) {
                Employe employe = employeOptional.get();
                displayName = employe.getNom() + " " + employe.getPrenom();
            }

        } else if (user.getRole() == Role.EMPLOYEUR) {
            Optional<Entreprise> entrepriseOptional = entrepriseRepository.findByEmployeurPrincipal(user);
            if (entrepriseOptional.isPresent()){
                Entreprise entreprise = entrepriseOptional.get();
                System.out.println("Entreprise trouvée pour l'utilisateur EMPLOYEUR: " + entreprise.getNom());
                displayName = user.getUsername() + " " + entreprise.getNom();
                entrepriseId = entreprise.getId();
            } else {
                System.out.println("Aucune entreprise trouvée pour l'utilisateur EMPLOYEUR: " + user.getUsername());
            }
        }
        AuthResponseDto authResponse = new AuthResponseDto(
                token,"Bearer",user.getUsername(),role ,displayName, entrepriseId
        );
        System.out.println("DisplayName final envoyé au frontend: " + authResponse.getDisplayName());

        return new ResponseEntity<>(new ApiResponse<>("Connexion reussie!",authResponse,HttpStatus.OK), HttpStatus.OK);


    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/register/employer-company", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Transactional
    public ResponseEntity<ApiResponse<String>> registerEmployerCompany (
            @RequestPart CreateEmployerAndCompanyDto createDto,
            @RequestPart (value = "logo", required = false )
            MultipartFile logoFile ){

        if(userRepository.existsByUsername(createDto.getUsername())){
            return new ResponseEntity<>(new ApiResponse<>("Nom d'utilisateur déjà pris!", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
        if (entrepriseRepository.findByNom(createDto.getNomEntreprise()).isPresent()) {
            return new ResponseEntity<>(new ApiResponse<>("Nom d'entreprise déjà utilisé!", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
        if (createDto.getNumeroSiret() != null && !createDto.getNumeroSiret().isEmpty() && entrepriseRepository.findByNumeroSiret(createDto.getNumeroSiret()).isPresent()) {
            return new ResponseEntity<>(new ApiResponse<>("Numéro SIRET déjà utilisé!", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
        try {
            Entreprise entreprise = Entreprise.builder()
                    .nom(createDto.getNomEntreprise())
                    .adresseEntreprise(createDto.getAdresseEntreprise())
                    .numeroSiret(createDto.getNumeroSiret())
                    .emailEntreprise(createDto.getEmailEntreprise())
                    .telephoneEntreprise(createDto.getTelephoneEntreprise())
                    .dateCreation(createDto.getDateCreation() != null? createDto.getDateCreation() : LocalDate.now())
                    .build();

            if(logoFile != null && !logoFile.isEmpty()){
                String logoUrl = fileStorageService.saveFile(logoFile);
                System.out.println("URL du logo générée par FileStorageService : " + logoUrl);
                entreprise.setLogoUrl(logoUrl);
            }

            Entreprise savedEntreprise = entrepriseRepository.save(entreprise);

            User employeurUser = User.builder()
                    .username(createDto.getUsername())
                    .password(passwordEncoder.encode(createDto.getPassword()))
                    .role(Role.EMPLOYEUR)
                    .entreprise(savedEntreprise)
                    .build();
            User savedUser =   userRepository.save(employeurUser);
            savedEntreprise.setEmployeurPrincipal(savedUser);
            entrepriseRepository.save(savedEntreprise);
            return new ResponseEntity<>(new ApiResponse<>("Entreprise et compte enregistre avec succes!",null,HttpStatus.CREATED), HttpStatus.CREATED);

        } catch (IOException e ){
            System.err.println("Erreur lors de l'upload du logo: " + e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Échec de l'enregistrement: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {

            System.err.println("Erreur inattendue lors de l'enregistrement de l'employeur: " + e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Une erreur est survenue lors de l'enregistrement: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }






    // pour cree juste employeur sans entreprise



    @PostMapping("/register/employee")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<Void>> registerEmployee(@RequestBody RegisterDto registerDto) {
        try {
            authService.registerEmployeeAccount(registerDto);
            return new ResponseEntity<>(new ApiResponse<>("Compte employe enregistre avec succes!", null, HttpStatus.CREATED), HttpStatus.CREATED);
        } catch (IllegalStateException | RessourceNotFoundException e) {
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage(), null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }
}
