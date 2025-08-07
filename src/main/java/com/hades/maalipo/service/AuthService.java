package com.hades.maalipo.service;

import com.hades.maalipo.dto.*;
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.*;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.repository.PasswordResetTokenRepository;
import com.hades.maalipo.repository.UserRepository;
import com.hades.maalipo.security.JwtTokenProvider;
import com.hades.maalipo.service.email.EmailService;
import com.hades.maalipo.service.pdf.UnifiedFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeRepository employeRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UnifiedFileStorageService unifiedFileStorageService;
    private final BulletinTemplateService bulletinTemplateService;
    private final EntrepriseParametreRhService entrepriseParametreRhService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;



    @Value("${app.frontend.password-reset-url}")
    private String frontendPasswordResetUrl;

    public AuthService(
            UserRepository userRepository,
            EmployeRepository employeRepository,
            EntrepriseRepository entrepriseRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UnifiedFileStorageService unifiedFileStorageService,
            BulletinTemplateService bulletinTemplateService,
             PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            AuditLogService auditLogService,
            EntrepriseParametreRhService entrepriseParametreRhService
            ) {


        this.userRepository = userRepository;
        this.employeRepository = employeRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.unifiedFileStorageService = unifiedFileStorageService;
        this.bulletinTemplateService = bulletinTemplateService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.entrepriseParametreRhService = entrepriseParametreRhService;
    }

    /**
     * Authentifie un utilisateur et génère un token JWT
     */
    public AuthResponseDto authenticateUser(LoginDto loginDto) {
        // Authentification
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);


        // Récupération de l'utilisateur
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if(user.getRole() == Role.EMPLOYEUR){
            if(user.getEntreprise() == null || !user.getEntreprise().isActive()){
                throw  new IllegalStateException("Le compte de votre entreprise est désactivé");
            }
        }


        // Génération du token
        String token = jwtTokenProvider.generateToken(authentication);

        // Détermination du nom d'affichage et de l'ID entreprise
        String role = user.getRole().name();
        String displayName = user.getUsername();
        Long entrepriseId = null;

        if (user.getRole() == Role.EMPLOYE) {
            displayName = getEmployeeDisplayName(user);
        } else if (user.getRole() == Role.EMPLOYEUR) {
            EmployerInfo employerInfo = getEmployerInfo(user);
            displayName = employerInfo.getDisplayName();
            entrepriseId = employerInfo.getEntrepriseId();
        } else if (user.getRole() == Role.ADMIN) {
            displayName = "Admin";
        }

        System.out.println("DisplayName final envoyé au frontend: " + displayName);

        return new AuthResponseDto(
                token, "Bearer", user.getUsername(), role, displayName, entrepriseId
        );
    }

    /**
     * Enregistre un employeur avec son entreprise
     */
    @Transactional
    public void registerEmployerCompany(CreateEmployerAndCompanyDto createDto, MultipartFile logoFile)
            throws IOException {

        // Validations
        validateEmployerCompanyRegistration(createDto);

        try {
            // Création de l'entreprise
            Entreprise entreprise = createEntreprise(createDto, logoFile);
            Entreprise savedEntreprise = entrepriseRepository.save(entreprise);
            auditLogService.logAction(
                    "CREATE_ENTREPRISE",
                    "Entreprise",
                    savedEntreprise.getId(),
                    auditLogService.getCurrentUsername(),
                    "Création de l'entreprise par admin"
            );
            // Création du template par défaut
            bulletinTemplateService.createDefaultTemplateForEntreprise(savedEntreprise);

            //creation des parametre par defaut
            entrepriseParametreRhService.initDefaultParamsForEntreprise(savedEntreprise.getId());

            // Création de l'utilisateur employeur
            User employeurUser = createEmployeurUser(createDto, savedEntreprise);
            User savedUser = userRepository.save(employeurUser);

            // Mise à jour de l'entreprise avec l'employeur principal
            savedEntreprise.setEmployeurPrincipal(savedUser);
            entrepriseRepository.save(savedEntreprise);
            auditLogService.logAction(
                    "CREATE_EMPLOYEUR_USER",
                    "User",
                    savedUser.getId(),
                    auditLogService.getCurrentUsername(),
                    "Création du compte employeur pour l'entreprise " + savedEntreprise.getNom()
            );

        } catch (IOException e) {
            System.err.println("Erreur lors de l'upload du logo: " + e.getMessage());
            throw new IOException("Échec de l'enregistrement: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de l'enregistrement de l'employeur: " + e.getMessage());
            throw new RuntimeException("Une erreur est survenue lors de l'enregistrement: " + e.getMessage(), e);
        }
    }

    /**
     * Enregistre un compte employé
     */
    @Transactional
    public void registerEmployeeAccount(RegisterDto registerDto) {
        Employe employe = employeRepository.findById(registerDto.getEmployeId())
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec ID: " + registerDto.getEmployeId()));

        // Vérifications
        if (userRepository.findByEmploye(employe).isPresent()) {
            throw new IllegalStateException("Cet employé a déjà un compte utilisateur.");
        }

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new IllegalStateException("Ce nom d'utilisateur est déjà pris.");
        }

        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(Role.EMPLOYE)
                .employe(employe)
                .build();

        employe.setUser(user);
        userRepository.save(user);
        auditLogService.logAction(
                "CREATE_EMPLOYE_USER",
                "User",
                user.getId(),
                auditLogService.getCurrentUsername(),
                "Création du compte employé pour l'employé " + employe.getNom()
        );
    }

    // Méthodes privées pour la logique métier

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = null;
        String recipientEmail = null;

        // Rechercher par email d'abord
        Optional<Employe> employeOptional = employeRepository.findByEmail(email);
        if (employeOptional.isPresent()) {
            user = employeOptional.get().getUser();
            recipientEmail = employeOptional.get().getEmail();
        } else {
            Optional<Entreprise> entrepriseOptional = entrepriseRepository.findByEmailEntreprise(email);
            if (entrepriseOptional.isPresent()) {
                Entreprise entreprise = entrepriseOptional.get();
                if (entreprise.getEmployeurPrincipal() != null) {
                    user = entreprise.getEmployeurPrincipal();
                    recipientEmail = entreprise.getEmailEntreprise();
                } else {
                    throw new RessourceNotFoundException("Entreprise trouvée mais sans employeur principal associé.");
                }
            }

        }
        //Vérifier la limitation de taux (3 tentatives par heure)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = passwordResetTokenRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);

        if (recentAttempts >= 3) {
            throw new IllegalStateException("Trop de tentatives de réinitialisation. Veuillez attendre une heure avant de réessayer.");
        }

        if (user == null || recipientEmail == null) {
            throw new RessourceNotFoundException("Utilisateur non trouvé ou adresse e-mail non disponible.");
        }

        // Supprimer tous les tokens existants pour cet utilisateur
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.flush();

        // Supprimer les tokens expirés
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        passwordResetTokenRepository.flush();

        // Créer un nouveau token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1); // 1 heure d'expiration

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        String resetLink = frontendPasswordResetUrl + "/" + token;
        emailService.sendPasswordResetEmail(recipientEmail, resetLink);

        System.out.println("Token de réinitialisation créé: " + token + " pour l'utilisateur: " + user.getUsername());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalStateException("Les mots de passe ne correspondent pas.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RessourceNotFoundException("Token de réinitialisation invalide ou introuvable."));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Supprimer le token expiré
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalStateException("Le lien de réinitialisation a expiré.");
        }

        User user = resetToken.getUser();


        // Encoder le nouveau mot de passe
        String newEncodedPassword = passwordEncoder.encode(newPassword);


        // Mettre à jour le mot de passe
        user.setPassword(newEncodedPassword);

        // Sauvegarder l'utilisateur avec flush pour forcer l'écriture en base
        User savedUser = userRepository.saveAndFlush(user);
        System.out.println("Mot de passe sauvé en base: " + savedUser.getPassword());

        // Supprimer TOUS les tokens de réinitialisation pour cet utilisateur
        passwordResetTokenRepository.deleteByUser(user);

        // Forcer le commit de la transaction
        passwordResetTokenRepository.flush();

        System.out.println("Réinitialisation terminée pour l'utilisateur: " + user.getUsername());
        auditLogService.logAction(
                "CHANGE_PASSWORD",
                "User",
                user.getId(),
                auditLogService.getCurrentUsername(),
                "Changement de mot de passe"
        );
    }

    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()){
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur authentifié non trouvé: " + username));

        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Le mot de passe actuel est incorrect.");
        }

        if(!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmNewPassword())){
            throw new IllegalStateException("Le nouveau mot de passe et sa confirmation ne correspondent pas.");
        }

        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())){
            throw new IllegalStateException("Le nouveau mot de passe ne peut pas être identique à l'ancien.");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDto.getConfirmNewPassword()));
        userRepository.save(user);
        auditLogService.logAction(
                "CHANGE_PASSWORD",
                "User",
                user.getId(),
                auditLogService.getCurrentUsername(),
                "Changement de mot de passe"
        );
    }

    private String getEmployeeDisplayName(User user) {
        Optional<Employe> employeOptional = employeRepository.findByUser(user);
        if (employeOptional.isPresent()) {
            Employe employe = employeOptional.get();
            return employe.getNom() + " " + employe.getPrenom();
        }
        return user.getUsername();
    }

    private EmployerInfo getEmployerInfo(User user) {
        Optional<Entreprise> entrepriseOptional = entrepriseRepository.findByEmployeurPrincipal(user);
        if (entrepriseOptional.isPresent()) {
            Entreprise entreprise = entrepriseOptional.get();
            // On retourne le nom de l'entreprise comme displayName
            String displayName = entreprise.getNom();
            return new EmployerInfo(displayName, entreprise.getId());
        } else {
            return new EmployerInfo(user.getUsername(), null);
        }
    }

    private void validateEmployerCompanyRegistration(CreateEmployerAndCompanyDto createDto) {
        if (userRepository.existsByUsername(createDto.getUsername())) {
            throw new IllegalStateException("Nom d'utilisateur déjà pris!");
        }

        if (entrepriseRepository.findByNom(createDto.getNomEntreprise()).isPresent()) {
            throw new IllegalStateException("Nom d'entreprise déjà utilisé!");
        }

        if (createDto.getNumeroSiret() != null && !createDto.getNumeroSiret().isEmpty()
                && entrepriseRepository.findByNumeroSiret(createDto.getNumeroSiret()).isPresent()) {
            throw new IllegalStateException("Numéro SIRET déjà utilisé!");
        }
    }

    private Entreprise createEntreprise(CreateEmployerAndCompanyDto createDto, MultipartFile logoFile)
            throws IOException {

        Entreprise entreprise = Entreprise.builder()
                .nom(createDto.getNomEntreprise())
                .adresseEntreprise(createDto.getAdresseEntreprise())
                .numeroSiret(createDto.getNumeroSiret())
                .emailEntreprise(createDto.getEmailEntreprise())
                .telephoneEntreprise(createDto.getTelephoneEntreprise())
                .dateCreation(createDto.getDateCreation() != null ? createDto.getDateCreation() : LocalDate.now())
                .standardHeuresHebdomadaires(createDto.getStandardHeuresHebdomadaires())
                .standardJoursOuvrablesHebdomadaires(createDto.getStandardJoursOuvrablesHebdomadaires())
                .build();

        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = unifiedFileStorageService.saveFile(logoFile);
            System.out.println("URL du logo générée par FileStorageService : " + logoUrl);
            entreprise.setLogoUrl(logoUrl);
        }

        return entreprise;
    }

    private User createEmployeurUser(CreateEmployerAndCompanyDto createDto, Entreprise entreprise) {
        return User.builder()
                .username(createDto.getUsername())
                .password(passwordEncoder.encode(createDto.getPassword()))
                .role(Role.EMPLOYEUR)
                .entreprise(entreprise)
                .build();
    }

    // Classe interne pour encapsuler les informations de l'employeur
    private static class EmployerInfo {
        private final String displayName;
        private final Long entrepriseId;

        public EmployerInfo(String displayName, Long entrepriseId) {
            this.displayName = displayName;
            this.entrepriseId = entrepriseId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Long getEntrepriseId() {
            return entrepriseId;
        }
    }
}