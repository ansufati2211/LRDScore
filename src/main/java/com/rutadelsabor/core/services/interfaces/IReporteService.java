    package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.dto.response.MargenVentasDTO;
import java.time.LocalDate;

public interface IReporteService {
    DashboardVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin);
    byte[] exportarVentasExcel(LocalDate inicio, LocalDate fin);
    // R5-2: solo disponible si el tenant tiene REPORTES_AVANZADOS habilitado
    MargenVentasDTO obtenerMargenVentas(LocalDate inicio, LocalDate fin);

}
