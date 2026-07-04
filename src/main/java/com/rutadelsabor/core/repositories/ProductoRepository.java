package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // R6-3/E6-3: para el reset masivo en cierre de caja
    List<Producto> findByEstadoDisponibilidad(EstadoDisponibilidad estado);

    // R6-5: porciones disponibles — solo productos activos
    List<Producto> findByEstadoRegistroTrue();
}
