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
// R2 (Módulo 2): CAJERO añadido al nivel de clase para que pueda suscribirse al canal SSE
// y recibir la escalada del nivel 2 (t=2min). Los endpoints /pendientes, /preparando y /listo
// tienen @PreAuthorize de método que siguen excluyendo a CAJERO y MOZO.
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA') or hasAuthority('ROLE_MOZO') or hasAuthority('ROLE_CAJERO')")
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
     * R2-2: Endpoint SSE. El empresaId del JWT validado determina el bucket de tenant
     * bajo el cual se registra el emitter — nunca de un parámetro de request.
     * Acepta ?token= como fallback para EventSource (que no puede enviar headers).
     *
     * Eventos emitidos al tenant:
     *   NUEVO_PEDIDO      → mozo confirma pedido (BORRADOR → RECIBIDO)
     *   EN_PREPARACION    → cocina inicia preparación
     *   PEDIDO_LISTO      → cocina marca plato listo (t=0 → al mozo creador)
     *   AVISO_PEDIDO_LISTO → escalación server-side (t=1min mozos, t=2min cajeros, t=5min gerentes)
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos(Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        return sseEmitterManager.suscribir(user.getEmpresaId(), user.getUsuarioId(), user.getRol());
    }
}
