package com.hades.maalipo.enum1;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ROLE_ADMIN"),
    EMPLOYE("ROLE_EMPLOYE"),
    EMPLOYEUR("ROLE_EMPLOYEUR");

    private  final String authority;
    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority(){
        return authority;
    }
}
