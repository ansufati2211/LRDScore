package com.rutadelsabor.core.config.tenant;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    // Constructor privado para evitar que alguien instancie esta clase de utilidad (java:S1118)
    private TenantContext() {
        throw new IllegalStateException("Clase de utilidad - no debe ser instanciada");
    }

    public static void setCurrentTenant(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}