package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.dto.response.KdsCocinaDTO;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map; // 🔥 Importación necesaria para el JSON de la receta

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

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<KdsCocinaDTO>> obtenerPendientes() {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes(TenantContext.getCurrentSede()));
    }

    @PutMapping("/{id}/preparando")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> marcarPreparando(@PathVariable Long id, Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl cocinero) {
            kdsService.marcarPreparando(id, cocinero.getUsuarioId());
            return ResponseEntity.ok("Plato en preparación. Stock descontado del Kardex.");
        }
        return ResponseEntity.badRequest().body("Usuario no autenticado");
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

    @PutMapping("/{id}/deshacer")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<String> deshacerPedido(@PathVariable("id") Long id) {
        kdsService.deshacerPedido(id);
        return ResponseEntity.ok("Pedido recuperado a la cocina exitosamente.");
    }

    @SuppressWarnings("squid:S6863")
    @GetMapping("/porciones")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<PorcionDisponibleDTO>> getPorcionesDisponibles() {
        try {
            Long sedeId = TenantContext.getCurrentSede();
            return ResponseEntity.ok(kdsService.calcularPorcionesDisponibles(sedeId));
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // 🔥 FIX: Añadimos el endpoint faltante para consultar la receta y sus ingredientes
    @GetMapping("/recetas/producto/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA', 'ROLE_MOZO')")
    public ResponseEntity<Map<String, Object>> obtenerRecetaKds(@PathVariable("id") Long id) {
        return ResponseEntity.ok(kdsService.obtenerRecetaKds(id));
    }

    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            return sseEmitterManager.suscribir(user.getEmpresaId(), user.getUsuarioId(), user.getRol(), TenantContext.getCurrentSede());
        }
        throw new SecurityException("Usuario no autenticado");
    }
}