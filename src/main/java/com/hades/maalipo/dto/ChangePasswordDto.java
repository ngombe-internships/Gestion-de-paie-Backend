package com.hades.maalipo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "Le mot de passe actuel est requis.")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Size(min =6, message = "Le nouveau mot de passe doit contenir au moins 6 caractéres.")
    private String newPassword;

    @NotBlank(message = "La confirmation du nouveau mot de passe est requise. ")
    private String confirmNewPassword;

}
