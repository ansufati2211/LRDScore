package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.models.entities.Suscripcion;
import com.rutadelsabor.core.models.enums.EstadoSuscripcion;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.repositories.SuscripcionRepository;
import com.rutadelsabor.core.services.interfaces.ISuscripcionService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SuscripcionServiceImpl implements ISuscripcionService {

    private final SuscripcionRepository suscripcionRepository;

    public SuscripcionServiceImpl(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    // R0-3: devuelve los módulos habilitados para el frontend en el response de login.
    // E0-1: si VENCIDA, solo módulos core (lectura); premium se ocultan.
    @Override
    public List<String> obtenerModulosHabilitados(Long empresaId) {
        return obtenerSuscripcionVigente(empresaId)
                .map(s -> {
                    if (s.getEstado() == EstadoSuscripcion.VENCIDA) {
                        return Arrays.stream(Modulo.values())
                                .filter(Modulo::esCore)
                                .map(Enum::name)
                                .toList();
                    }
                    return s.getPlan().getModulos().stream()
                            .map(pm -> pm.getCodigoModulo().name())
                            .toList();
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public Optional<Suscripcion> obtenerSuscripcionVigente(Long empresaId) {
        return suscripcionRepository.findFirstByEmpresaIdAndEstadoInOrderByFechaInicioDesc(
                empresaId,
                List.of(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.VENCIDA)
        );
    }
}
