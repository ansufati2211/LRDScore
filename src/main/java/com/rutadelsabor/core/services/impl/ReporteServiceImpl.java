package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import com.rutadelsabor.core.repositories.VwDashboardVentasRepository;
import com.rutadelsabor.core.services.interfaces.IReporteService;
import com.rutadelsabor.core.services.reportes.ExcelReportManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteServiceImpl implements IReporteService {

    private final VwDashboardVentasRepository dashboardRepository;
    private final ExcelReportManager excelManager;

    // Inyección por Constructor
    public ReporteServiceImpl(VwDashboardVentasRepository dashboardRepository, ExcelReportManager excelManager) {
        this.dashboardRepository = dashboardRepository;
        this.excelManager = excelManager;
    }

    @Override
    public DashboardVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        List<VwDashboardVentas> ventas = dashboardRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin);

        DashboardVentasDTO dto = new DashboardVentasDTO();
        
        // Sumar totales usando Streams de forma funcional
        BigDecimal totalIngresos = ventas.stream()
                .map(VwDashboardVentas::getTotalIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // SOLUCIÓN AL ERROR: Cambiamos mapToInt por mapToLong y usamos Long
        Long totalPedidos = ventas.stream()
                .mapToLong(VwDashboardVentas::getCantidadPedidos)
                .sum();

        // Mapeo y recolección optimizada con .toList() nativo de Java
        List<DashboardVentasDTO.DetalleDiaDTO> detalle = ventas.stream().map(v -> {
            DashboardVentasDTO.DetalleDiaDTO d = new DashboardVentasDTO.DetalleDiaDTO();
            d.setFecha(v.getFecha().toString());
            d.setIngresos(v.getTotalIngresos());
            d.setPedidos(v.getCantidadPedidos());
            return d;
        }).toList();

        dto.setIngresosTotalesMensuales(totalIngresos);
        dto.setPedidosTotalesMensuales(totalPedidos);
        dto.setDetalleDiario(detalle);

        return dto;
    }

    @Override
    public byte[] exportarVentasExcel(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        List<VwDashboardVentas> ventas = dashboardRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin);
        return excelManager.generarReporteVentas(ventas);
    }

    /**
     * Valida de manera defensiva que los parámetros de consulta de tiempo sean coherentes.
     * Genera una excepción de negocio interceptable por el GlobalExceptionHandler de la Sede.
     */
    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Las fechas de inicio y fin para la generación del reporte no pueden ser nulas.");
        }
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("Inconsistencia de rango: La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha de fin (" + fin + ").");
        }
    }
}