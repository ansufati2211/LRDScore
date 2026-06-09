package com.rutadelsabor.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(Authentication authentication) {
        // Validación defensiva explícita (Resuelve definitivamente java:S2259)
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal)) {
            throw new IllegalArgumentException("El objeto de autenticación provisto no contiene un principal válido");
        }

        Date fechaEmision = new Date();
        Date fechaExpiracion = new Date(fechaEmision.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("rol", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .claim("empresaId", userPrincipal.getEmpresaId()) // ¡Dato SaaS Crítico!
                .claim("usuarioId", userPrincipal.getUsuarioId())
                .issuedAt(fechaEmision)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public Long getEmpresaIdFromToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().get("empresaId", Long.class);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token JWT inválido, manipulado o expirado: {}", e.getMessage());
        }
        return false;
    }
}