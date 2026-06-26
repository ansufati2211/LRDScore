package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kds")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA')")
public class KdsController {

    private final IKdsService kdsService;

    public KdsController(IKdsService kdsService) {
        this.kdsService = kdsService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<VwKdsCocina>> obtenerPendientes() {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes());
    }

    @PutMapping("/{id}/preparando")
    public ResponseEntity<String> marcarPreparando(@PathVariable Long id, Authentication auth) {
        UserDetailsImpl cocinero = (UserDetailsImpl) auth.getPrincipal();
        kdsService.marcarPreparando(id, cocinero.getUsuarioId());
        return ResponseEntity.ok("Plato en preparación. Stock descontado del Kardex.");
    }

    @PutMapping("/{id}/listo")
    public ResponseEntity<String> marcarListo(@PathVariable Long id) {
        kdsService.marcarListo(id);
        return ResponseEntity.ok("Plato listo. El mozo ha sido notificado.");
    }
}