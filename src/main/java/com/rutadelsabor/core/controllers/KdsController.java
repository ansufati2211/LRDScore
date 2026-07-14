package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.JwtProvider;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/kds")
public class KdsController {

    private final IKdsService kdsService;
    private final SseEmitterManager sseEmitterManager;
    private final JwtProvider jwtProvider;
    private final UsuarioRepository usuarioRepository;

    public KdsController(IKdsService kdsService, SseEmitterManager sseEmitterManager, JwtProvider jwtProvider, UsuarioRepository usuarioRepository) {
        this.kdsService = kdsService;
        this.sseEmitterManager = sseEmitterManager;
        this.jwtProvider = jwtProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> obtenerPedidosPendientes(@RequestParam(required = false) Long sedeId) {
        try {
            return ResponseEntity.ok(kdsService.obtenerPedidosPendientes(sedeId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener KDS: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/preparando")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> marcarPreparando(@PathVariable Long id, Authentication auth) {
        try {
            Long usuarioId = obtenerUsuarioAutenticado(auth).getUsuarioId();
            kdsService.marcarPreparando(id, usuarioId);
            return ResponseEntity.ok(Map.of("message", "Preparación iniciada"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Fallo al iniciar preparación: " + e.getMessage(),
                    "detalles", e.getCause() != null ? e.getCause().getMessage() : "Regla de negocio"
            ));
        }
    }

    @PutMapping("/{id}/listo")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> marcarListo(@PathVariable Long id) {
        try {
            kdsService.marcarListo(id);
            return ResponseEntity.ok(Map.of("message", "Pedido listo"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Fallo al marcar listo: " + e.getMessage()
            ));
        }
    }

    // 🔥 NUEVO: Botón Deshacer
    @PutMapping("/{id}/deshacer")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> deshacerPedido(@PathVariable Long id) {
        try {
            kdsService.deshacerPedido(id);
            return ResponseEntity.ok(Map.of("message", "El pedido regresó a cocina exitosamente."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Fallo al deshacer: " + e.getMessage()
            ));
        }
    }

    // 🔥 NUEVO: Visor de Recetas
    @GetMapping("/recetas/producto/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> obtenerRecetaKds(@PathVariable Long productoId) {
        try {
            return ResponseEntity.ok(kdsService.obtenerRecetaKds(productoId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "Error al buscar receta: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/porciones")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<?> calcularPorcionesDisponibles(@RequestParam(required = false) Long sedeId) {
        try {
            return ResponseEntity.ok(kdsService.calcularPorcionesDisponibles(sedeId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error calculando porciones: " + e.getMessage()));
        }
    }

    // ⚠️ ATENCIÓN: Rutas ajustadas para coincidir con React (/productos/{id}/...)
    @PutMapping("/productos/{productoId}/agotado-temporal")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<?> marcarAgotadoTemporal(@PathVariable Long productoId) {
        try {
            kdsService.marcarAgotadoTemporal(productoId);
            return ResponseEntity.ok(Map.of("message", "Marcado como Agotado Temporal"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/productos/{productoId}/agotado-servicio")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<?> marcarAgotadoServicio(@PathVariable Long productoId) {
        try {
            kdsService.marcarAgotadoServicio(productoId);
            return ResponseEntity.ok(Map.of("message", "Marcado como 86 Definitivo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/productos/{productoId}/disponible")
    @PreAuthorize("hasAnyAuthority('ROLE_COCINA', 'ROLE_GERENTE_SEDE', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<?> revertirDisponible(@PathVariable Long productoId) {
        try {
            kdsService.revertirDisponible(productoId);
            return ResponseEntity.ok(Map.of("message", "Nuevamente disponible"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

@GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos(@RequestParam("token") String token) {
        String correo = jwtProvider.extractUsername(token);
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        // Extraemos la Sede directamente del token en lugar del Usuario
        Long sedeId = jwtProvider.extractSedeId(token); 
        
        return sseEmitterManager.suscribir(u.getEmpresaId(), sedeId, u.getRol(), u.getId());
    }

    private UserDetailsImpl obtenerUsuarioAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            throw new IllegalStateException("Identidad no disponible");
        }
        return user;
    }
}