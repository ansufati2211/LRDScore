package com.rutadelsabor.core.config.tenant;

import com.rutadelsabor.core.exceptions.ReglaNegocioException;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_SEDE = new ThreadLocal<>();

    // Constructor privado para ocultar el implícito público (Buena práctica)
    private TenantContext() {}

    public static void setCurrentTenant(Long tenantId) { CURRENT_TENANT.set(tenantId); }
    public static Long getCurrentTenant() { return CURRENT_TENANT.get(); }

    public static void setCurrentSede(Long sedeId) { CURRENT_SEDE.set(sedeId); }
    public static Long getCurrentSede() { return CURRENT_SEDE.get(); }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_SEDE.remove();
    }

    // FASE 3: Resolución inteligente de Sede para Escrituras
    public static Long resolverSedeEfectiva(Long sedeIdDelRequest) {
        Long sedeSesion = getCurrentSede();
        if (sedeSesion != null) {
            // GERENTE_SEDE o personal operativo: Ignoramos el request, su sede manda.
            return sedeSesion;
        }
        // ADMIN_EMPRESA o SUPER_ADMIN: Al no tener sede fija, deben indicar dónde quieren operar.
        if (sedeIdDelRequest == null) {
            throw new ReglaNegocioException("Debe especificar la sede para esta operación.");
        }
        return sedeIdDelRequest;
    }
}