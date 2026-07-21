package com.rutadelsabor.core.security;

import com.rutadelsabor.core.config.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    public JwtRequestFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jwt = extraerJwt(request);

        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarUsuario(jwt, request);
        }
        
        chain.doFilter(request, response);
    }

    private String extraerJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        return null;
    }

    private void autenticarUsuario(String jwt, HttpServletRequest request) {
        try {
            String username = jwtProvider.extractUsername(jwt);
            if (username != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtProvider.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    TenantContext.setCurrentTenant(jwtProvider.extractEmpresaId(jwt));
                    Long sedeId = jwtProvider.extractSedeId(jwt);
                    if (sedeId != null) {
                        TenantContext.setCurrentSede(sedeId);
                    }
                }
            }
        } catch (JwtException e) {
            logger.warn("Petición rechazada: Token JWT inválido o expirado. " + e.getMessage());
        }
    }
}