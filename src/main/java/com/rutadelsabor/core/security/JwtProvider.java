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
@SuppressWarnings("java:S2143") // Se suprime porque la librería io.jsonwebtoken obliga a usar java.util.Date
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    // R1-3: token de larga duración (30 días por defecto) para la cuenta kiosco de cocina.
    // Usar long porque 30 días en ms (2 592 000 000) supera Integer.MAX_VALUE (2 147 483 647).
    @Value("${jwt.expiration-cocina}")
    private long jwtExpirationCocinaMs;

    // Genera la llave segura a partir de la propiedad Base64
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generador de Token (usado en el AuthController)
    public String generateToken(Authentication authentication, Long empresaId, Long usuarioId) {
        // Solución java:S2259 - Extracción segura del Principal
        UserDetailsImpl userPrincipal = obtenerPrincipalSeguro(authentication);
        
        String rol = userPrincipal.getAuthorities().iterator().next().getAuthority();
        // R1-3: la cuenta kiosco de cocina recibe token de larga duración para no interrumpir el servicio.
        long expMs = "ROLE_COCINA".equals(rol) ? jwtExpirationCocinaMs : jwtExpirationMs;

        // Solución java:S2143 - Uso de java.time.Instant para matemáticas de fechas
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("rol", rol)
                .claim("empresaId", empresaId)
                .claim("usuarioId", usuarioId)
            .issuedAt(java.util.Date.from(now)) // Puente de Instant a Date exigido por JJWT
            .expiration(java.util.Date.from(expiration)) // Puente de Instant a Date exigido por JJWT
                .signWith(getSigningKey())
                .compact();
    }

    // --- MÉTODOS PARA EL FILTRO (Extractores) ---
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractEmpresaId(String token) {
        return extractClaim(token, claims -> claims.get("empresaId", Long.class));
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Funciones utilitarias internas
    private boolean isTokenExpired(String token) {
        Instant expirationDate = extractClaim(token, claims -> {
            java.util.Date exp = claims.getExpiration();
            return exp == null ? null : exp.toInstant();
        });
        if (expirationDate == null) return true;
        return expirationDate.isBefore(Instant.now());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Método auxiliar para validar el Authentication y extraer el UserDetailsImpl
     * de forma segura, resolviendo la vulnerabilidad de NullPointerException (java:S2259).
     */
    private UserDetailsImpl obtenerPrincipalSeguro(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal)) {
            throw new IllegalStateException("Autenticación nula o inválida al intentar generar el token JWT");
        }
        return userPrincipal;
    }
}