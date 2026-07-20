package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.dto.response.KdsCocinaDTO; // <-- NUEVA IMPORTACIÓN
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
@RequestMapping("/api/kds")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA', 'ROLE_MOZO', 'ROLE_CAJERO')")
public class KdsController {
    private final IKdsService kdsService;
    private final SseEmitterManager sseEmitterManager;

    public KdsController(IKdsService kdsService, SseEmitterManager sseEmitterManager) {
        this.kdsService = kdsService;
        this.sseEmitterManager = sseEmitterManager;
    }

    // CORRECCIÓN: Retorna KdsCocinaDTO en lugar de VwKdsCocina
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<KdsCocinaDTO>> obtenerPendientes() {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes(TenantContext.getCurrentSede()));
    }

    @PutMapping("/{id}/preparando")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> marcarPreparando(@PathVariable Long id, Authentication auth) {
        UserDetailsImpl cocinero = (UserDetailsImpl) auth.getPrincipal();
        kdsService.marcarPreparando(id, cocinero.getUsuarioId());
        return ResponseEntity.ok("Plato en preparación. Stock descontado del Kardex.");
    }

    @PutMapping("/{id}/listo")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> marcarListo(@PathVariable Long id) {
        kdsService.marcarListo(id);
        return ResponseEntity.ok("Plato listo. Mozo notificado en tiempo real.");
    }

    @PutMapping("/productos/{productoId}/agotado-temporal")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> marcarAgotadoTemporal(@PathVariable Long productoId) {
        kdsService.marcarAgotadoTemporal(productoId);
        return ResponseEntity.ok("Producto marcado como AGOTADO_TEMPORAL.");
    }

    @PutMapping("/productos/{productoId}/agotado-servicio")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> marcarAgotadoServicio(@PathVariable Long productoId) {
        kdsService.marcarAgotadoServicio(productoId);
        return ResponseEntity.ok("Producto marcado como AGOTADO_SERVICIO.");
    }

    @PutMapping("/productos/{productoId}/disponible")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> revertirDisponible(@PathVariable Long productoId) {
        kdsService.revertirDisponible(productoId);
        return ResponseEntity.ok("Producto restablecido a DISPONIBLE.");
    }

    @GetMapping("/productos/porciones")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA', 'ROLE_MOZO')")
    public ResponseEntity<List<PorcionDisponibleDTO>> obtenerPorciones() {
        return ResponseEntity.ok(kdsService.calcularPorcionesDisponibles(TenantContext.getCurrentSede()));
    }

    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos(Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        return sseEmitterManager.suscribir(user.getEmpresaId(), user.getUsuarioId(), user.getRol(), TenantContext.getCurrentSede());
    }
}