package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.annotations.RequiereModulo;
import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.dto.response.MargenVentasDTO;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.services.interfaces.IReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
public class ReporteController {

    // 1. Declaramos la dependencia como final e inmutable
    private final IReporteService reporteService;

    // 2. Inyección por Constructor (Resuelve de raíz java:S6813)
    // Spring Boot inyectará automáticamente el Bean correspondiente en tiempo de ejecución.
    public ReporteController(IReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // Endpoint para el gráfico de React
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardVentasDTO> obtenerDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        
        return ResponseEntity.ok(reporteService.obtenerResumenVentas(inicio, fin));
    }

    // R5-2/E5-3: gateado por ModuloInterceptor — lanza ModuloNoHabilitadoException si el plan no incluye REPORTES_AVANZADOS
    @GetMapping("/margen")
    @RequiereModulo(Modulo.REPORTES_AVANZADOS)
    public ResponseEntity<MargenVentasDTO> obtenerMargen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        return ResponseEntity.ok(reporteService.obtenerMargenVentas(inicio, fin));
    }

    // Endpoint para descargar el Excel
    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        byte[] excelBytes = reporteService.exportarVentasExcel(inicio, fin);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}