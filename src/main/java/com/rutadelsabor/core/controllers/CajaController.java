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
@RequestMapping("/api/caja")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
public class CajaController {

    private final ICajaService cajaService;

    public CajaController(ICajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<SesionCaja> abrirCaja(@RequestBody SesionCajaRequestDTO dto, Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.abrirCaja(usuarioId, dto.getMontoInicial()));
    }

    @PutMapping("/cerrar/{id}")
    public ResponseEntity<SesionCaja> cerrarCaja(@PathVariable Long id, @RequestBody SesionCajaRequestDTO dto) {
        return ResponseEntity.ok(cajaService.cerrarCaja(id, dto.getMontoFinalDeclarado()));
    }

    @GetMapping("/activa")
    public ResponseEntity<SesionCaja> obtenerCajaActiva(Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.obtenerCajaActivaPorCajero(usuarioId));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<SesionCaja>> listarHistorial(Authentication auth) {
        Long usuarioId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(cajaService.listarHistorialPorCajero(usuarioId));
    }

    /**
     * Método auxiliar para extraer el ID del usuario de forma segura 
     * y evitar advertencias de NullPointerException (java:S2259).
     */
    private Long obtenerUsuarioIdAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl cajero)) {
            // Se puede lanzar tu ReglaNegocioException si prefieres un manejo global de errores
            throw new IllegalStateException("No se pudo obtener la identidad del usuario autenticado");
        }
        return cajero.getUsuarioId();
    }
}