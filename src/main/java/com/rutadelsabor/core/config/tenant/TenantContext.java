package com.rutadelsabor.core.config.tenant;

import com.rutadelsabor.core.exceptions.ReglaNegocioException; // <-- Nueva importación

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

    public static Long resolverSedeEfectiva(Long sedeSolicitada) {
        Long sedeActual = getCurrentSede();
        if (sedeActual != null) {
            return sedeActual;
        }
        if (sedeSolicitada != null) {
            return sedeSolicitada;
        }
        // FIX: Cambiamos a ReglaNegocioException para que retorne un HTTP 400 en vez de 500
        throw new ReglaNegocioException("Error de Contexto: Falta especificar la sede. Por favor envía el Header 'X-Sede-ID' (ej. 1) o el parámetro 'sedeId' en el JSON.");
    }
}