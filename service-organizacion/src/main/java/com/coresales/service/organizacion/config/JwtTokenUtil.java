package com.coresales.service.organizacion.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;


@Component
public class JwtTokenUtil {
    private final Key key;

    public JwtTokenUtil(@Value("${app.jwt.secret}") String secret){
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public boolean validateToken(String token){
        Jwts.parserBuilder().setSigningKey(key)
                .build().parseClaimsJws(token)
                .getBody().getSubject();

        return true;
    }

    public String getUsernameFromToken(String token){
        return Jwts.parserBuilder().setSigningKey(key)
                .build().parseClaimsJws(token)
                .getBody().getSubject();
    }
}
