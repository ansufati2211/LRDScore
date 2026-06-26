package com.rutadelsabor.core.config.security;

import com.rutadelsabor.core.config.tenant.TenantInterceptor;
import com.rutadelsabor.core.interceptors.ModuloInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
        // R0-4: ModuloInterceptor evalúa @RequiereModulo después del JWT filter
        registry.addInterceptor(moduloInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 2. Orígenes específicos en lugar de "*" (Resuelve java:S5122)
                // Aquí agregamos los puertos típicos de desarrollo de React (3000) y Vite (5173).
                // Cuando subas tu frontend a internet, solo agregas tu URL de Vercel/Netlify aquí.
                .allowedOrigins(
                        "http://localhost:3000", 
                        "http://localhost:5173",
                        "https://larutadelsabor-frontend.vercel.app" // Ejemplo de producción
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Empresa-ID", "Accept")
                .allowCredentials(true);
    }
}