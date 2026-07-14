package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.annotations.RequiereModulo;
import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.dto.response.MargenVentasDTO;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.services.interfaces.IReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final IReporteService reporteService;

    public ReporteController(IReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ventas/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    @RequiereModulo(Modulo.REPORTES_AVANZADOS)
    public ResponseEntity<DashboardVentasDTO> obtenerDashboardVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(reporteService.obtenerResumenVentas(inicio, fin, sedeId));
    }

    @GetMapping("/ventas/exportar-excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    @RequiereModulo(Modulo.REPORTES_AVANZADOS)
    public ResponseEntity<byte[]> exportarVentasExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        
        byte[] excelBytes = reporteService.exportarVentasExcel(inicio, fin, sedeId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte_ventas.xlsx");
        headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/ventas/margen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    @RequiereModulo(Modulo.REPORTES_AVANZADOS)
    public ResponseEntity<MargenVentasDTO> obtenerMargenVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(reporteService.obtenerMargenVentas(inicio, fin, sedeId));
    }
}