package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface VwDashboardVentasRepository extends JpaRepository<VwDashboardVentas, Long> {
    List<VwDashboardVentas> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);
}