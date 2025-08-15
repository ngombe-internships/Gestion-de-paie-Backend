package com.hades.maalipo.dto.authen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {

    private String token;
    private String type = "Bearer";
    private String username;
    private String role;
    private String displayName;
    private Long entrepriseId;
}
