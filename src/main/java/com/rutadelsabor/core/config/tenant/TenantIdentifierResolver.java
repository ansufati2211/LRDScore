package com.rutadelsabor.core.config.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }
        // Si alguien hace una petición sin estar logueado (ej. página pública)
        // Devolvemos -1L para que no choque con empresas reales.
        return -1L; 
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}