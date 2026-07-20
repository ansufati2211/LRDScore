package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.*;
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

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        Usuario mozo = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mozo no encontrado"));
        return ResponseEntity.ok(pedidoService.crearPedido(dto, mozo));
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<String> confirmarPedido(@PathVariable Long id) {
        pedidoService.confirmarPedido(id);
        return ResponseEntity.ok("Pedido confirmado y enviado a cocina.");
    }

    @PutMapping("/{id}/entregar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<String> entregarPedido(@PathVariable Long id) {
        pedidoService.entregarPedido(id);
        return ResponseEntity.ok("Pedido marcado como entregado a la mesa.");
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<String> procesarPago(@PathVariable Long id, @RequestBody PagoRequestDTO pago, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        pedidoService.procesarPago(id, pago, cajero.getUsuarioId());
        return ResponseEntity.ok("Pago registrado exitosamente.");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO', 'ROLE_COCINA')")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO', 'ROLE_COCINA')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarPedidosActivos(@RequestParam(required = false) Long sedeId) {
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
    public ResponseEntity<Pedido> aplicarDescuento(@PathVariable Long id, @RequestParam BigDecimal monto) {
        return ResponseEntity.ok(pedidoService.aplicarDescuento(id, monto));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> cancelarPedido(@PathVariable Long id) {
        pedidoService.cancelarPedido(id);
        return ResponseEntity.ok("Pedido anulado exitosamente.");
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
    public ResponseEntity<String> agregarItems(@PathVariable Long id, @RequestBody AgregarItemsRequestDTO dto) {
        pedidoService.agregarItemsAPedido(id, dto);
        return ResponseEntity.ok("Ítems agregados y enviados a cocina.");
    }

    @PutMapping("/{id}/items/{detalleId}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO')")
    public ResponseEntity<String> cancelarItem(@PathVariable Long id,
                                               @PathVariable Long detalleId,
                                               @RequestBody(required = false) CancelarItemRequestDTO dto,
                                               Authentication auth) {
        boolean esGerente = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE_SEDE")
                            || a.getAuthority().equals("ROLE_ADMIN_EMPRESA")
                            || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String motivo = dto != null ? dto.getMotivo() : null;
        pedidoService.cancelarItem(id, detalleId, motivo, esGerente);
        return ResponseEntity.ok("Ítem cancelado.");
    }

    @PostMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> crearDocumentoCobro(@PathVariable Long id, @RequestBody DocumentoCobroRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.crearDocumentoCobro(id, dto));
    }

    @GetMapping("/{id}/documentos-cobro")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoCobroResponseDTO>> listarDocumentosCobro(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listarDocumentosCobro(id));
    }

    @PostMapping("/documentos-cobro/{documentoId}/pagar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoCobroResponseDTO> pagarDocumentoCobro(@PathVariable Long documentoId, @RequestBody PagoRequestDTO pago, Authentication auth) {
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(pedidoService.pagarDocumentoCobro(documentoId, pago, cajero.getUsuarioId()));
    }
}