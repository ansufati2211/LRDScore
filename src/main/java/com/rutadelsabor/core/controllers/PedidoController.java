package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import com.rutadelsabor.core.services.reportes.TicketManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        Usuario mozo = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario mozo autenticado no encontrado con el correo: " + auth.getName()));
                
        return ResponseEntity.ok(pedidoService.crearPedido(dto, mozo));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<String> procesarPago(
            @PathVariable Long id, 
            @RequestBody PagoRequestDTO pago, 
            Authentication auth) {
        
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl cajero)) {
            throw new ReglaNegocioException("Acceso denegado: Usuario no autenticado o token inválido.");
        }
        
        pedidoService.procesarPago(id, pago, cajero.getUsuarioId());
        
        return ResponseEntity.ok("Pago registrado exitosamente. Stock actualizado en sede.");
    }

    @GetMapping("/{id}/ticket")
    public ResponseEntity<String> imprimirTicket(@PathVariable Long id) {
        Pedido pedido = pedidoService.obtenerPedido(id);
        String comprobante = ticketManager.generarTicketTermico(pedido);
        
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(comprobante);
    }

    // NUEVO ENDPOINT 1: Lista todos los pedidos "vivos" para que el cajero sepa qué cobrar
    @GetMapping("/activos")
    public ResponseEntity<List<Pedido>> listarPedidosActivos() {
        return ResponseEntity.ok(pedidoService.listarPedidosActivos());
    }

    // NUEVO ENDPOINT 2: Expone a la red la anulación de un pedido
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<String> cancelarPedido(@PathVariable Long id) {
        pedidoService.cancelarPedido(id);
        return ResponseEntity.ok("El pedido ha sido anulado y retirado de la cola de preparación.");
    }
}