package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Suscripcion;
import com.rutadelsabor.core.models.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    // Devuelve la suscripción más reciente con estado ACTIVA o VENCIDA para la empresa
    Optional<Suscripcion> findFirstByEmpresaIdAndEstadoInOrderByFechaInicioDesc(
            Long empresaId, Collection<EstadoSuscripcion> estados);

    // Para el response de login: solo la suscripción ACTIVA
    Optional<Suscripcion> findFirstByEmpresaIdAndEstadoOrderByFechaInicioDesc(
            Long empresaId, EstadoSuscripcion estado);
}
