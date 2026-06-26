package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import com.rutadelsabor.core.services.reportes.TicketManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        Usuario mozo = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mozo no encontrado"));
        return ResponseEntity.ok(pedidoService.crearPedido(dto, mozo));
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<String> confirmarPedido(@PathVariable Long id) {
        pedidoService.confirmarPedido(id);
        return ResponseEntity.ok("Pedido confirmado y enviado a cocina.");
    }

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
        UserDetailsImpl cajero = (UserDetailsImpl) auth.getPrincipal();
        pedidoService.procesarPago(id, pago, cajero.getUsuarioId());
        return ResponseEntity.ok("Pago registrado exitosamente.");
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO') or hasAuthority('ROLE_MOZO') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<List<PedidoActivoResponseDTO>> listarPedidosActivos() {
        return ResponseEntity.ok(pedidoService.listarPedidosActivos());
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

    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<String> imprimirTicket(@PathVariable Long id) {
        Pedido pedido = pedidoService.obtenerPedido(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(ticketManager.generarTicketTermico(pedido));
    }
}