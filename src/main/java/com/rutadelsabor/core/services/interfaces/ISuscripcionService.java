package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.Suscripcion;

import java.util.List;
import java.util.Optional;

public interface ISuscripcionService {

    // Módulos accesibles del tenant: plan completo si ACTIVA, solo core si VENCIDA, vacío si no hay suscripción
    List<String> obtenerModulosHabilitados(Long empresaId);

    // Suscripción vigente (ACTIVA o VENCIDA). Vacío si solo existe SUSPENDIDA o ninguna.
    Optional<Suscripcion> obtenerSuscripcionVigente(Long empresaId);
}
