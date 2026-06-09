package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.SesionCajaRequestDTO;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    // 1. Variable final y sin @Autowired
    private final ICajaService cajaService;

    // 2. Inyección por Constructor (Resuelve java:S6813)
    public CajaController(ICajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<SesionCaja> abrirCaja(@RequestBody SesionCajaRequestDTO request) {
        SesionCaja sesion = cajaService.abrirCaja(request.getCajeroId(), request.getMontoInicial());
        return new ResponseEntity<>(sesion, HttpStatus.CREATED);
    }

    @PutMapping("/cerrar/{id}")
    public ResponseEntity<SesionCaja> cerrarCaja(
            @PathVariable Long id, 
            @RequestBody SesionCajaRequestDTO request) {
        SesionCaja sesion = cajaService.cerrarCaja(id, request.getMontoFinalDeclarado());
        return ResponseEntity.ok(sesion);
    }

    @GetMapping("/activa/{cajeroId}")
    public ResponseEntity<SesionCaja> obtenerCajaActiva(@PathVariable Long cajeroId) {
        return ResponseEntity.ok(cajaService.obtenerCajaActiva(cajeroId));
    }
}