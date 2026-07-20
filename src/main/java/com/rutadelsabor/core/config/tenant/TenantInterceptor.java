package com.rutadelsabor.core.config.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String headerSede = request.getHeader("X-Sede-ID");
        if (headerSede != null && !headerSede.isBlank()) {
            TenantContext.setCurrentSede(Long.valueOf(headerSede));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // JwtRequestFilter ya limpia el TenantContext, se gestiona ahí.
    }
}