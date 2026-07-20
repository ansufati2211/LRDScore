package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.annotations.RequiereModulo;
import com.rutadelsabor.core.config.tenant.TenantContext;
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
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
public class ReporteController {
    private final IReporteService reporteService;

    public ReporteController(IReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // CORRECCIÓN: Inyecta la sede desde el contexto
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardVentasDTO> obtenerDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reporteService.obtenerResumenVentas(inicio, fin, TenantContext.getCurrentSede()));
    }

    // CORRECCIÓN: Inyecta la sede desde el contexto
    @GetMapping("/margen")
    @RequiereModulo(Modulo.REPORTES_AVANZADOS)
    public ResponseEntity<MargenVentasDTO> obtenerMargen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reporteService.obtenerMargenVentas(inicio, fin, TenantContext.getCurrentSede()));
    }

    // CORRECCIÓN: Inyecta la sede desde el contexto
    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        byte[] excelBytes = reporteService.exportarVentasExcel(inicio, fin, TenantContext.getCurrentSede());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}