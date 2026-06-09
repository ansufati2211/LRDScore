package com.rutadelsabor.core.config.security;

import com.rutadelsabor.core.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Eliminamos el @Autowired de campo (java:S6813)
    
    @Bean
    // Inyectamos JwtRequestFilter directamente como parámetro (java:S3305)
    // Eliminamos el "throws Exception" (java:S1130)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) {
        try {
            http
                // Desactivamos CSRF porque usaremos JWT (Stateless)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Le decimos a Spring que no guarde sesiones en memoria
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Configuramos qué rutas son públicas y cuáles privadas
                .authorizeHttpRequests(auth -> auth
                    // Rutas públicas (Login, Webhooks de IA y Documentación Swagger)
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/webhook/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    
                    // Cualquier otra ruta requiere estar autenticado con JWT
                    .anyRequest().authenticated()
                );

            // AQUÍ inyectamos el filtro JWT
            http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
            
        } catch (Exception e) {
            // Atrapamos la excepción genérica y lanzamos una específica (java:S112)
            throw new IllegalStateException("Error al construir la cadena de seguridad HTTP", e);
        }
    }

    @Bean
    // Eliminamos el "throws Exception" de la firma
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            // Atrapamos la excepción genérica y lanzamos una específica (java:S112)
            throw new IllegalStateException("Error al obtener el AuthenticationManager de Spring Security", e);
        }
    }
}