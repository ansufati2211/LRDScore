package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VwDashboardVentasRepository extends JpaRepository<VwDashboardVentas, LocalDate> {
    List<VwDashboardVentas> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);
}