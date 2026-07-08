package com.rutadelsabor.core.config.tenant;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_SEDE = new ThreadLocal<>();

    private TenantContext() {
        throw new IllegalStateException("Clase de utilidad - no debe ser instanciada");
    }

    public static void setCurrentTenant(Long tenantId) { CURRENT_TENANT.set(tenantId); }
    public static Long getCurrentTenant() { return CURRENT_TENANT.get(); }

    public static void setCurrentSede(Long sedeId) { CURRENT_SEDE.set(sedeId); }
    public static Long getCurrentSede() { return CURRENT_SEDE.get(); }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_SEDE.remove();
    }
}