package com.hades.paie1.dto;

import com.hades.paie1.model.Entreprise;
import lombok.Data;

@Data
public class RegisterDto {

    private String username;
    private String password;
    private Long   employeId;


}
