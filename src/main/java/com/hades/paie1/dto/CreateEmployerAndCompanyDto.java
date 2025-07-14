package com.hades.paie1.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data

public class CreateEmployerAndCompanyDto {
        private  String username;
        private  String password;


        //info entreprise
        private String nomEntreprise;
        private String adresseEntreprise;
        private String numeroSiret;
        private LocalDate dateCreation;
        private String emailEntreprise;
        private String telephoneEntreprise;

        //nouveau
        private Double latitudeEntreprise;
        private Double longitudeEntreprise;
        private Integer radiusToleranceMeters;
        private BigDecimal standardHeuresHebdomadaires;
        private Integer standardJoursOuvrablesHebdomadaires;

}
