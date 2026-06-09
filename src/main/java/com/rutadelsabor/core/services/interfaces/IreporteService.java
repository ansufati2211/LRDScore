package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import java.time.LocalDate;

public interface IReporteService {
    DashboardVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin);
    byte[] exportarVentasExcel(LocalDate inicio, LocalDate fin);
}