package com.rutadelsabor.core.security;

import com.rutadelsabor.core.config.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j // 1. Soporte para Logs profesionales (Resuelve java:S106)
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // Variables finales e inmutables sin @Autowired de campo (Resuelve java:S6813)
    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    // 2. Inyección por Constructor: Alta cohesión arquitectónica
    public JwtRequestFilter(JwtProvider jwtProvider, UserDetailsServiceImpl userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtProvider.validateJwtToken(jwt)) {
                
                String username = jwtProvider.getUsernameFromToken(jwt);
                Long empresaId = jwtProvider.getEmpresaIdFromToken(jwt);

                // 1. INYECCIÓN MULTI-TENANT
                if (empresaId != null) {
                    TenantContext.setCurrentTenant(empresaId);
                }

                // 2. AUTENTICACIÓN SPRING SECURITY
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Registro asíncrono y seguro en el Logger del sistema (Resuelve java:S106)
            log.error("Error en la autenticación del usuario a nivel de filtro de red: {}", e.getMessage());
        } finally {
            // 3. LIMPIEZA DE HILO (Movido al bloque finally para garantizar la ejecución pase lo que pase)
            TenantContext.clear();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Quita la palabra "Bearer "
        }
        return null;
    }
}