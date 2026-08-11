package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByEstadoDisponibilidad(EstadoDisponibilidad estado);

    List<Producto> findByEstadoRegistroTrue();
}
