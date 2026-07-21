package com.rutadelsabor.core.config.security;

import com.rutadelsabor.core.config.tenant.TenantInterceptor;
import com.rutadelsabor.core.interceptors.ModuloInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final ModuloInterceptor moduloInterceptor;

    public CorsConfig(TenantInterceptor tenantInterceptor, ModuloInterceptor moduloInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.moduloInterceptor = moduloInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(moduloInterceptor).addPathPatterns("/api/**");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*", 
                "http://127.0.0.1:*",
                "https://larutadelsabor-frontend.vercel.app"
        ));
        
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // SOLUCIÓN NUCLEAR CORS: Permitir TODOS los headers entrantes (*)
        config.setAllowedHeaders(Collections.singletonList("*"));
        
        // También exponemos los headers por si el frontend necesita leerlos
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Sede-ID"));
        
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}