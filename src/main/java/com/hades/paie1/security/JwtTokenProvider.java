package com.hades.paie1.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.*;


@Component
public class JwtTokenProvider {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds}")
    private long jwtExpirationDate;

    //generer jwt
    public String generateToken (Authentication authentication) {
        String username = authentication.getName();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key ())
                .compact();
        return token;
    }

    //obtenir la cle signature du jwt
    private Key key (){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    //Obtenir le nom d'utilisateur a partir du jwt
    public String getUsername(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken (String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        } catch (MalformedJwtException e){
            System.err.println("jeton JWT invalide: " +e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Le jeton JWT a expirer" + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Jeton JWT non supporté: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("La chaîne de revendications JWT est vide: " + e.getMessage());
        }
        return false;
    }


}
