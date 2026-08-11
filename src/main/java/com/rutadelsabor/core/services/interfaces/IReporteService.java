package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.dto.response.MargenVentasDTO;
import java.time.LocalDate;

public interface IReporteService {
    DashboardVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin, Long sedeId);
    byte[] exportarVentasExcel(LocalDate inicio, LocalDate fin, Long sedeId);
    MargenVentasDTO obtenerMargenVentas(LocalDate inicio, LocalDate fin, Long sedeId);
}