package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.SesionCajaRequestDTO;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
public class CajaController {

    private final ICajaService cajaService;

    public CajaController(ICajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<SesionCaja> abrirCaja(@RequestBody SesionCajaRequestDTO dto, Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.abrirCaja(usuarioId, dto.getMontoInicial(), dto.getSedeId()));
    }

    @PutMapping("/cerrar/{id}")
    public ResponseEntity<SesionCaja> cerrarCaja(@PathVariable Long id, @RequestBody SesionCajaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.cerrarCaja(id, dto.getMontoFinalDeclarado()));
    }

    @GetMapping("/activa")
    public ResponseEntity<SesionCaja> obtenerCajaActiva(
            @RequestParam(required = false) Long sedeId,
            Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.obtenerCajaActivaPorCajero(usuarioId, sedeId));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<SesionCaja>> listarHistorial(
            @RequestParam(required = false) Long sedeId,
            Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.listarHistorialPorCajero(usuarioId, sedeId));
    }

    private Long obtenerUsuarioIdAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl cajero)) {
            throw new IllegalStateException("No se pudo obtener la identidad del usuario autenticado");
        }
        return cajero.getUsuarioId();
    }
}