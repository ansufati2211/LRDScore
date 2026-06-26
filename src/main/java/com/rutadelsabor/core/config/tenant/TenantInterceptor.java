package com.rutadelsabor.core.config.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    // El tenant se establece EXCLUSIVAMENTE desde el JWT en JwtRequestFilter.
    // Este interceptor no lee ni sobreescribe el tenant: hacerlo permitiría que
    // cualquier cliente enviara X-Empresa-ID arbitrario y accediera a datos de otra empresa.

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // La limpieza del TenantContext la gestiona JwtRequestFilter en su bloque finally.
    }
}