package com.hades.paie1.enum1;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ROLE_ADMIN"),
    EMPLOYE("ROLE_EMPLOYE");

    private  final String authority;
    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority(){
        return authority;
    }
}
