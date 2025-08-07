package com.hades.maalipo.controller;

import com.hades.maalipo.dto.*;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint de connexion
     */
    @PostMapping("login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> authenticateUser(@RequestBody LoginDto loginDto) {
        try {
            AuthResponseDto authResponse = authService.authenticateUser(loginDto);
            return new ResponseEntity<>(
                    new ApiResponse<>("Connexion réussie!", authResponse, HttpStatus.OK),
                    HttpStatus.OK
            );
        }  catch (BadCredentialsException e) { // Capture les erreurs d'authentification (mauvais mot de passe)
            return new ResponseEntity<>(
                    new ApiResponse<>("Nom d'utilisateur ou mot de passe incorrect.", null, HttpStatus.UNAUTHORIZED),
                    HttpStatus.UNAUTHORIZED
            );
        } catch (IllegalStateException e) { // Capture les erreurs logiques que vous avez définies dans le service
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.FORBIDDEN),
                    HttpStatus.FORBIDDEN
            );
        } catch (Exception e) { // Capture toutes les autres exceptions inattendues
            return new ResponseEntity<>(
                    new ApiResponse<>("Une erreur inattendue est survenue: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Endpoint pour enregistrer un employeur avec son entreprise
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/register/employer-company", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<String>> registerEmployerCompany(
            @RequestPart CreateEmployerAndCompanyDto createDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {

        try {
            authService.registerEmployerCompany(createDto, logoFile);
            return new ResponseEntity<>(
                    new ApiResponse<>("Entreprise et compte enregistrés avec succès!", null, HttpStatus.CREATED),
                    HttpStatus.CREATED
            );
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.BAD_REQUEST),
                    HttpStatus.BAD_REQUEST
            );
        } catch (IOException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Erreur lors de l'upload du fichier: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Une erreur inattendue est survenue: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Endpoint pour enregistrer un employé
     */
    @PostMapping("/register/employee")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<Void>> registerEmployee(@RequestBody RegisterDto registerDto) {
        try {
            authService.registerEmployeeAccount(registerDto);
            return new ResponseEntity<>(
                    new ApiResponse<>("Compte employé enregistré avec succès!", null, HttpStatus.CREATED),
                    HttpStatus.CREATED
            );
        } catch (IllegalStateException | RessourceNotFoundException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.BAD_REQUEST),
                    HttpStatus.BAD_REQUEST
            );
        }
    }


    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody  ChangePasswordDto changePasswordDto){
        try {
            authService.changePassword(changePasswordDto);
            return new ResponseEntity<>(
                    new ApiResponse<>("Votre mot de passe a été change avec succés.",null,HttpStatus.OK),
                    HttpStatus.OK
            );
        } catch (BadCredentialsException e){
            return new ResponseEntity<>(
                    new ApiResponse<>( e.getMessage(),null, HttpStatus.UNAUTHORIZED),
            HttpStatus.UNAUTHORIZED
            );
        } catch (IllegalStateException | RessourceNotFoundException e) {
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(),null,HttpStatus.BAD_REQUEST),
                            HttpStatus.BAD_REQUEST
                    );
        }  catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse<>("Une erreur inattendue est survenue lors du changement de mot de passe : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @PostMapping("/password-reset-request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@RequestBody PasswordResetRequestDto requestDto){
       try {
           authService.initiatePasswordReset(requestDto.getEmailOrUsername());

           return new ResponseEntity<>(
                   new ApiResponse<>("Si votre compte existe, un lien de réinitialisation a été envoyé à votre adresse e-mail.", null, HttpStatus.OK),
                   HttpStatus.OK
           );
       } catch (RessourceNotFoundException e) {
           // Même message générique en cas de RessourceNotFoundException (utilisateur non trouvé)
           return new ResponseEntity<>(
                   new ApiResponse<>("Si votre compte existe, un lien de réinitialisation a été envoyé à votre adresse e-mail.", null, HttpStatus.OK),
                   HttpStatus.OK
           );
       } catch (MailException e) {
           // Gérer les échecs d'envoi d'e-mail spécifiquement
           return new ResponseEntity<>(
                   new ApiResponse<>("Erreur lors de l'envoi de l'e-mail de réinitialisation. Veuillez réessayer plus tard.", null, HttpStatus.INTERNAL_SERVER_ERROR),
                   HttpStatus.INTERNAL_SERVER_ERROR
           );
       } catch (Exception e) {
           // Capturer toute autre exception inattendue
           return new ResponseEntity<>(
                   new ApiResponse<>("Une erreur inattendue est survenue lors de la demande de réinitialisation : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                   HttpStatus.INTERNAL_SERVER_ERROR
           );
       }
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody PasswordResetDto resetDto) {
        try {
            authService.resetPassword(resetDto.getToken(), resetDto.getNewPassword(), resetDto.getConfirmPassword());
            return new ResponseEntity<>(
                    new ApiResponse<>("Votre mot de passe a été réinitialisé avec succès.", null, HttpStatus.OK),
                    HttpStatus.OK
            );
        } catch (IllegalStateException | RessourceNotFoundException e) {
            // Erreurs spécifiques comme "mots de passe ne correspondent pas", "token expiré/invalide"
            return new ResponseEntity<>(
                    new ApiResponse<>(e.getMessage(), null, HttpStatus.BAD_REQUEST),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            // Capturer toute autre erreur inattendue
            return new ResponseEntity<>(
                    new ApiResponse<>("Une erreur inattendue est survenue lors de la réinitialisation du mot de passe : " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }


    }


}