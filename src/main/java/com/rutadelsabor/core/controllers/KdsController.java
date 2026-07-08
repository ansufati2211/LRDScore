package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.dto.response.KdsCocinaDTO;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kds")
public class KdsController {

    private final IKdsService kdsService;
    private final SseEmitterManager sseEmitterManager;

    public KdsController(IKdsService kdsService, SseEmitterManager sseEmitterManager) {
        this.kdsService = kdsService;
        this.sseEmitterManager = sseEmitterManager;
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<KdsCocinaDTO>> obtenerPedidosPendientes(@RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes(sedeId));
    }

    @PutMapping("/{id}/preparando")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<Void> marcarPreparando(@PathVariable Long id, Authentication auth) {
        kdsService.marcarPreparando(id, obtenerUsuarioAutenticado(auth).getUsuarioId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/listo")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<Void> marcarListo(@PathVariable Long id) {
        kdsService.marcarListo(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/porciones")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<PorcionDisponibleDTO>> calcularPorcionesDisponibles(@RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(kdsService.calcularPorcionesDisponibles(sedeId));
    }

    @PutMapping("/agotado-temporal/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Void> marcarAgotadoTemporal(@PathVariable Long productoId) {
        kdsService.marcarAgotadoTemporal(productoId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/agotado-servicio/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Void> marcarAgotadoServicio(@PathVariable Long productoId) {
        kdsService.marcarAgotadoServicio(productoId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/disponible/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Void> revertirDisponible(@PathVariable Long productoId) {
        kdsService.revertirDisponible(productoId);
        return ResponseEntity.ok().build();
    }

    // FASE 7: Suscripción incluyendo la Sede
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA', 'ROLE_MOZO', 'ROLE_CAJERO')")
    public SseEmitter suscribirEventos(Authentication auth) {
        UserDetailsImpl user = obtenerUsuarioAutenticado(auth);
        return sseEmitterManager.suscribir(user.getEmpresaId(), user.getUsuarioId(), user.getRol(), user.getSedeId());
    }

    private UserDetailsImpl obtenerUsuarioAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            throw new IllegalStateException("Identidad no disponible");
        }
        return user;
    }
}