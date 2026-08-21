package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    List<Reserva> findBySedeIdAndFechaHoraBetweenOrderByFechaHoraAsc(Long sedeId, LocalDateTime inicio, LocalDateTime fin);

    List<Reserva> findBySedeIdOrderByFechaHoraAsc(Long sedeId);
}