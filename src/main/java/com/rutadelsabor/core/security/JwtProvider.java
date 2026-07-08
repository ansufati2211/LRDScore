package com.rutadelsabor.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.function.Function;

@Component
@SuppressWarnings("java:S2143") 
public class JwtProvider {

    @Value("${jwt.secret}") private String jwtSecret;
    @Value("${jwt.expiration}") private int jwtExpirationMs;
    @Value("${jwt.expiration-cocina}") private long jwtExpirationCocinaMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Firma actualizada para inyectar sedeId
    public String generateToken(Authentication authentication, Long empresaId, Long usuarioId, Long sedeId) {
        UserDetailsImpl userPrincipal = obtenerPrincipalSeguro(authentication);
        String rol = userPrincipal.getAuthorities().iterator().next().getAuthority();
        long expMs = "ROLE_COCINA".equals(rol) ? jwtExpirationCocinaMs : jwtExpirationMs;

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("rol", rol)
                .claim("empresaId", empresaId)
                .claim("usuarioId", usuarioId)
                .claim("sedeId", sedeId) // INYECTADO
            .issuedAt(java.util.Date.from(now)) 
            .expiration(java.util.Date.from(expiration)) 
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public Long extractEmpresaId(String token) { return extractClaim(token, claims -> claims.get("empresaId", Long.class)); }
    
    // Extractor del SedeId
    public Long extractSedeId(String token) { return extractClaim(token, claims -> claims.get("sedeId", Long.class)); }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        Instant expirationDate = extractClaim(token, claims -> {
            java.util.Date exp = claims.getExpiration();
            return exp == null ? null : exp.toInstant();
        });
        return expirationDate == null || expirationDate.isBefore(Instant.now());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
    
    private UserDetailsImpl obtenerPrincipalSeguro(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal)) {
            throw new IllegalStateException("Autenticación inválida");
        }
        return userPrincipal;
    }
}