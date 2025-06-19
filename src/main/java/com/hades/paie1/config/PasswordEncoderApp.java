package com.hades.paie1.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderApp {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "adminpass";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Mot de passe encodé pour '" + rawPassword + "': " + encodedPassword);
    }
}