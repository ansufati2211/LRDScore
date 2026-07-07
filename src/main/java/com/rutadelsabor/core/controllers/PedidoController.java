package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.AgregarItemsRequestDTO;
import com.rutadelsabor.core.dto.request.CancelarItemRequestDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final IPedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final TicketManager ticketManager;

    public PedidoController(IPedidoService pedidoService, UsuarioRepository usuarioRepository, TicketManager ticketManager) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
        this.ticketManager = ticketManager;
    }

    // R1-1 (CRÍTICA): ROLE_COCINA excluido explícitamente — única línea de defensa dura en modo kiosco.
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        Usuario mozo = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mozo no encontrado"));
        return ResponseEntity.ok(pedidoService.crearPedido(dto, mozo));
    }

    // R1-1: ROLE_COCINA excluido — solo puede cambiar estados en /api/kds/*, no confirmar pedidos.
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<String> confirmarPedido(@PathVariable Long id) {
        pedidoService.confirmarPedido(id);
        return ResponseEntity.ok("Pedido confirmado y enviado a cocina.");
    }

    // R1-1: ROLE_COCINA excluido — la entrega la marca el mozo, no la cocina.
    @PutMapping("/{id}/entregar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<String> entregarPedido(@PathVariable Long id) {
        pedidoService.entregarPedido(id);
        return ResponseEntity.ok("Pedido marcado como entregado a la mesa.");
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<String> procesarPago(
            @PathVariable Long id, 
            @RequestBody PagoRequestDTO pago, 
            Authentication auth) {
        Long cajeroId = obtenerUsuarioIdAutenticado(auth);
        pedidoService.procesarPago(id, pago, cajeroId);
        return ResponseEntity.ok("Pago registrado exitosamente.");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO') or hasAuthority('ROLE_MOZO') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarPedidosActivos() {
        return ResponseEntity.ok(pedidoService.listarPedidosActivos());
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarHistorial(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(pedidoService.listarHistorial(inicio, fin));
    }

    @PutMapping("/{id}/descuento")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> aplicarDescuento(@PathVariable Long id, @RequestParam BigDecimal monto) {
        pedidoService.aplicarDescuento(id, monto);
        return ResponseEntity.ok("Descuento de S/ " + monto + " aplicado al pedido.");
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> cancelarPedido(@PathVariable Long id) {
        pedidoService.cancelarPedido(id);
        return ResponseEntity.ok("Pedido anulado exitosamente.");
    }

    // R2-5: ACK informacional — el cliente del mozo confirma que recibió el aviso PEDIDO_LISTO.
    // No detiene la cascada de escalación server-side, que corre por tiempo independientemente.
    @PostMapping("/{id}/notificacion/ack")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<Void> ackNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<String> imprimirTicket(@PathVariable Long id) {
        Pedido pedido = pedidoService.obtenerPedido(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(ticketManager.generarTicketTermico(pedido));
    }

    // --- MÓDULO 4 ---

    // R4-1/R4-2: agregar ítems a un pedido confirmado; reserva y notifica solo los nuevos
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<String> agregarItems(@PathVariable Long id,
                                               @RequestBody AgregarItemsRequestDTO dto) {
        pedidoService.agregarItemsAPedido(id, dto);
        return ResponseEntity.ok("Ítems agregados y enviados a cocina.");
    }

    // R4-5/R4-6/E4-1: cancelar ítem individual; ENTREGADO requiere GERENTE y motivo
    @PutMapping("/{id}/items/{detalleId}/cancelar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<String> cancelarItem(@PathVariable Long id,
                                               @PathVariable Long detalleId,
                                               @RequestBody(required = false) CancelarItemRequestDTO dto,
                                               Authentication auth) {
        boolean esGerente = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE")
                            || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String motivo = dto != null ? dto.getMotivo() : null;
        pedidoService.cancelarItem(id, detalleId, motivo, esGerente);
        return ResponseEntity.ok("Ítem cancelado.");
    }

    // R4-3: crear documento de cobro (split por ítem o por monto)
    @PostMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> crearDocumentoCobro(
            @PathVariable Long id,
            @RequestBody DocumentoCobroRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.crearDocumentoCobro(id, dto));
    }

    // R4-3/R4-4: listar documentos de cobro de un pedido
    @GetMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoCobroResponseDTO>> listarDocumentosCobro(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listarDocumentosCobro(id));
    }

    // R4-4/E4-3: pagar un documento de cobro individual
    @PostMapping("/documentos-cobro/{documentoId}/pagar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> pagarDocumentoCobro(
            @PathVariable Long documentoId,
            @RequestBody PagoRequestDTO pago,
            Authentication auth) {
        Long cajeroId = obtenerUsuarioIdAutenticado(auth);
        return ResponseEntity.ok(pedidoService.pagarDocumentoCobro(documentoId, pago, cajeroId));
    }

    /**
     * Método auxiliar para validar el Authentication y extraer el ID del usuario
     * de forma segura, resolviendo java:S2259.
     */
    private Long obtenerUsuarioIdAutenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl cajero)) {
            throw new IllegalStateException("No se pudo obtener la identidad del usuario autenticado");
        }
        return cajero.getUsuarioId();
    }
}