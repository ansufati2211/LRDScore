package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.Suscripcion;

import java.util.List;
import java.util.Optional;

public interface ISuscripcionService {

    List<String> obtenerModulosHabilitados(Long empresaId);

    Optional<Suscripcion> obtenerSuscripcionVigente(Long empresaId);
}
