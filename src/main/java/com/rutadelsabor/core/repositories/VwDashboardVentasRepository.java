package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface VwDashboardVentasRepository extends JpaRepository<VwDashboardVentas, Long> {
    
    // Para ver el dashboard de una sola sede:
    List<VwDashboardVentas> findBySedeIdAndFechaBetweenOrderByFechaAsc(Long sedeId, LocalDate inicio, LocalDate fin);
    
    // Para ver el consolidado de toda la cadena (Uso exclusivo del ROLE_ADMIN_EMPRESA):
    List<VwDashboardVentas> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);
}