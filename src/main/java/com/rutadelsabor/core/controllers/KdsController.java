package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kds")
public class KdsController {

    // 1. Declaramos la dependencia como final e inmutable
    private final IKdsService kdsService;

    // 2. Inyección por Constructor (Resuelve java:S6813)
    // Spring inyectará automáticamente la implementación IKdsService aquí
    public KdsController(IKdsService kdsService) {
        this.kdsService = kdsService;
    }

    // Endpoint para que la pantalla se actualice cada X segundos
    @GetMapping("/pendientes")
    public ResponseEntity<List<VwKdsCocina>> verPedidosPendientes() {
        return ResponseEntity.ok(kdsService.obtenerPedidosPendientes());
    }

    // El cocinero toca el ticket para avisar que empezó a cocinarlo
    @PutMapping("/{pedidoId}/preparando")
    public ResponseEntity<String> iniciarPreparacion(@PathVariable Long pedidoId) {
        kdsService.marcarPreparando(pedidoId);
        return ResponseEntity.ok("Pedido marcado como EN PREPARACIÓN");
    }

    // El cocinero toca el ticket para avisar que el mozo ya puede recogerlo
    @PutMapping("/{pedidoId}/listo")
    public ResponseEntity<String> finalizarPreparacion(@PathVariable Long pedidoId) {
        kdsService.marcarListo(pedidoId);
        return ResponseEntity.ok("Pedido marcado como LISTO para entregar al cliente");
    }
}