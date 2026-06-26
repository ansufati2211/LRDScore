package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.SesionCajaRequestDTO;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
public class CajaController {

    private final ICajaService cajaService;

    public CajaController(ICajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<SesionCaja> abrirCaja(@RequestBody SesionCajaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.abrirCaja(dto.getCajeroId(), dto.getMontoInicial()));
    }

    @PutMapping("/cerrar/{id}")
    public ResponseEntity<SesionCaja> cerrarCaja(@PathVariable Long id, @RequestBody SesionCajaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.cerrarCaja(id, dto.getMontoFinalDeclarado()));
    }

    @GetMapping("/activa/{cajeroId}")
    public ResponseEntity<SesionCaja> obtenerCajaActiva(@PathVariable Long cajeroId) {
        return ResponseEntity.ok(cajaService.obtenerCajaActivaPorCajero(cajeroId));
    }
}