package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Suscripcion;
import com.rutadelsabor.core.models.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    Optional<Suscripcion> findFirstByEmpresaIdAndEstadoInOrderByFechaInicioDesc(
            Long empresaId, Collection<EstadoSuscripcion> estados);

    Optional<Suscripcion> findFirstByEmpresaIdAndEstadoOrderByFechaInicioDesc(
            Long empresaId, EstadoSuscripcion estado);
}
