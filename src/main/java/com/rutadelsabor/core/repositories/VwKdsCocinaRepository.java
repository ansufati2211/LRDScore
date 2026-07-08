package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwKdsCocinaRepository extends JpaRepository<VwKdsCocina, Long> {
    // Para el empleado local
    List<VwKdsCocina> findBySedeIdOrderByHoraIngresoAsc(Long sedeId);
    
    // Para el Administrador global
    List<VwKdsCocina> findAllByOrderByHoraIngresoAsc();
}