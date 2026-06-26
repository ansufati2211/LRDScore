package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.models.entities.VwKdsCocina;
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
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA') or hasAuthority('ROLE_MOZO')")
public class KdsController {

    private final IKdsService kdsService;
    private final SseEmitterManager sseEmitterManager;

    public KdsController(IKdsService kdsService, SseEmitterManager sseEmitterManager) {
        this.kdsService = kdsService;
        this.sseEmitterManager = sseEmitterManager;
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<List<VwKdsCocina>> obtenerPendientes() {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes());
    }

    @PutMapping("/{id}/preparando")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<String> marcarPreparando(@PathVariable Long id, Authentication auth) {
        UserDetailsImpl cocinero = (UserDetailsImpl) auth.getPrincipal();
        kdsService.marcarPreparando(id, cocinero.getUsuarioId());
        return ResponseEntity.ok("Plato en preparación. Stock descontado del Kardex.");
    }

    @PutMapping("/{id}/listo")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<String> marcarListo(@PathVariable Long id) {
        kdsService.marcarListo(id);
        return ResponseEntity.ok("Plato listo. Mozo notificado en tiempo real.");
    }

    /**
     * Endpoint SSE: el cliente (cocina/sala) se suscribe y recibe eventos push.
     * Eventos emitidos:
     *   - NUEVO_PEDIDO    → cuando un mozo confirma un pedido (BORRADOR → RECIBIDO)
     *   - EN_PREPARACION  → cuando cocina inicia la preparación
     *   - PEDIDO_LISTO    → cuando cocina marca el plato como listo
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos() {
        return sseEmitterManager.suscribir();
    }
}
