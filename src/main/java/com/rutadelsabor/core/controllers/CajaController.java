package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.SesionCajaRequestDTO;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
public class CajaController {

    private final ICajaService cajaService;

    public CajaController(ICajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<SesionCaja> abrirCaja(@RequestBody SesionCajaRequestDTO dto, @RequestParam(required = false) Long sedeId, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(cajaService.abrirCaja(cajero.getUsuarioId(), dto.getMontoInicial(), sedeId));
    }

    @PutMapping("/cerrar/{id}")
    public ResponseEntity<SesionCaja> cerrarCaja(@PathVariable Long id, @RequestBody SesionCajaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.cerrarCaja(id, dto.getMontoFinalDeclarado()));
    }

    @GetMapping("/activa")
    public ResponseEntity<SesionCaja> obtenerCajaActiva(@RequestParam(required = false) Long sedeId, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(cajaService.obtenerCajaActivaPorCajero(cajero.getUsuarioId(), sedeId));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<SesionCaja>> listarHistorial(@RequestParam(required = false) Long sedeId, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(cajaService.listarHistorialPorCajero(cajero.getUsuarioId(), sedeId));
    }

    @GetMapping("/activa/resumen")
    public ResponseEntity<Map<String, BigDecimal>> obtenerResumenCajaActiva(@RequestParam(required = false) Long sedeId, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(cajaService.obtenerResumenCajaActiva(cajero.getUsuarioId(), sedeId));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<List<SesionCaja>> listarAuditoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(cajaService.listarAuditoriaCajas(inicio, fin, sedeId));
    }

    @GetMapping("/auditoria/excel")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<byte[]> descargarExcelAuditoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        
        byte[] excelBytes = cajaService.exportarAuditoriaExcel(inicio, fin, sedeId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria_cajas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}