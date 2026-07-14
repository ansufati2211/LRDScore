package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.AgregarItemsRequestDTO;
import com.rutadelsabor.core.dto.request.DocumentoCobroRequestDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoCobroResponseDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import com.rutadelsabor.core.services.reportes.TicketManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final IPedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final TicketManager ticketManager;

    public PedidoController(IPedidoService pedidoService, UsuarioRepository usuarioRepository, TicketManager ticketManager) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
        this.ticketManager = ticketManager;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<?> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        try {
            Usuario mozo = usuarioRepository.findByCorreo(auth.getName())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Mozo no encontrado"));
            Pedido pedido = pedidoService.crearPedido(dto, mozo);
            return ResponseEntity.ok(Map.of("message", "Pedido creado exitosamente", "id", pedido.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear: " + e.getMessage()));
        }
    }

    // 🔥 MODO DEBUG ACTIVADO PARA REVELAR EL ERROR 500 EXACTO 🔥
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<?> confirmarPedido(@PathVariable Long id) {
        try {
            pedidoService.confirmarPedido(id);
            return ResponseEntity.ok(Map.of("message", "Pedido confirmado y enviado a cocina."));
        } catch (Exception e) {
            e.printStackTrace(); // Imprime la traza roja en la consola de Java (IntelliJ/Eclipse)
            String mensajePincipal = e.getMessage() != null ? e.getMessage() : "Error Nulo";
            String causaRaiz = e.getCause() != null ? e.getCause().getMessage() : "Sin causa raíz";
            
            // Le mandamos a React exactamente qué falló
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Fallo en Backend al confirmar: " + mensajePincipal,
                    "detalles", causaRaiz
            ));
        }
    }

@PutMapping("/{id}/entregar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<?> entregarPedido(@PathVariable Long id) {
        try {
            pedidoService.entregarPedido(id);
            return ResponseEntity.ok(Map.of("message", "Pedido marcado como entregado a la mesa."));
        } catch (Exception e) {
            e.printStackTrace();
            String mensajePincipal = e.getMessage() != null ? e.getMessage() : "Error Nulo";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Fallo al entregar: " + mensajePincipal,
                    "detalles", e.getCause() != null ? e.getCause().getMessage() : "Regla de negocio no cumplida"
            ));
        }
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<Map<String, String>> procesarPago(
            @PathVariable Long id, 
            @RequestBody PagoRequestDTO pago, 
            Authentication auth) {
        Long cajeroId = obtenerUsuarioIdAutenticado(auth);
        pedidoService.procesarPago(id, pago, cajeroId);
        return ResponseEntity.ok(Map.of("message", "Pago registrado exitosamente."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO')")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO', 'ROLE_COCINA')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarPedidosActivos(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(pedidoService.listarPedidosActivos(sedeId));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarHistorial(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(pedidoService.listarHistorial(inicio, fin, sedeId));
    }

    @PutMapping("/{id}/descuento")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Map<String, String>> aplicarDescuento(@PathVariable Long id, @RequestParam BigDecimal monto) {
        pedidoService.aplicarDescuento(id, monto);
        return ResponseEntity.ok(Map.of("message", "Descuento de S/ " + monto + " aplicado al pedido."));
    }

    // 🔥 MODO DEBUG ACTIVADO PARA CANCELAR 🔥
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO', 'ROLE_CAJERO')")
    public ResponseEntity<?> cancelarPedido(@PathVariable Long id) {
        try {
            pedidoService.cancelarPedido(id);
            return ResponseEntity.ok(Map.of("message", "Pedido anulado exitosamente."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Fallo al cancelar: " + e.getMessage()
            ));
        }
    }
    

    @PostMapping("/{id}/notificacion/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO', 'ROLE_CAJERO')")
    public ResponseEntity<Void> ackNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<String> imprimirTicket(@PathVariable Long id) {
        Pedido pedido = pedidoService.obtenerPedido(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(ticketManager.generarTicketTermico(pedido));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<Map<String, String>> agregarItems(@PathVariable Long id,
                                               @RequestBody AgregarItemsRequestDTO dto) {
        pedidoService.agregarItemsAPedido(id, dto);
        return ResponseEntity.ok(Map.of("message", "Ítems agregados y enviados a cocina."));
    }

    @PutMapping("/{id}/items/{detalleId}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<?> cancelarItem(@PathVariable Long id,
                                               @PathVariable Long detalleId,
                                               @RequestBody(required = false) Map<String, String> payload,
                                               Authentication auth) {
        try {
            boolean esGerente = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE_SEDE")
                                || a.getAuthority().equals("ROLE_ADMIN_EMPRESA")
                                || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
                                
            String motivo = payload != null ? payload.get("motivo") : null;
            pedidoService.cancelarItem(id, detalleId, motivo, esGerente);
            return ResponseEntity.ok(Map.of("message", "Ítems cancelados."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Fallo al cancelar el ítem: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> crearDocumentoCobro(
            @PathVariable Long id,
            @RequestBody DocumentoCobroRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.crearDocumentoCobro(id, dto));
    }

    @GetMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoCobroResponseDTO>> listarDocumentosCobro(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listarDocumentosCobro(id));
    }

    @PostMapping("/documentos-cobro/{documentoId}/pagar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> pagarDocumentoCobro(
            @PathVariable Long documentoId,
            @RequestBody PagoRequestDTO pago,
            Authentication auth) {
        Long cajeroId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(pedidoService.pagarDocumentoCobro(documentoId, pago, cajeroId));
    }

    private Long obtenerUsuarioIdAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl cajero)) {
            throw new IllegalStateException("No se pudo obtener la identidad del usuario autenticado");
        }
        return cajero.getUsuarioId();
    }
}