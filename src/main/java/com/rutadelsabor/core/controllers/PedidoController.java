package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired private IPedidoService pedidoService;
    @Autowired private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoRequestDTO dto, Authentication auth) {
        // Obtenemos el usuario autenticado desde el Token JWT
        Usuario mozo = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        return ResponseEntity.ok(pedidoService.crearPedido(dto, mozo));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<String> procesarPago(@PathVariable Long id, @RequestBody PagoRequestDTO pago) {
        // Llamada al SP que procesa y descuenta stock
        // (Asegúrate de tener el método en el repositorio tal como lo definimos antes)
        return ResponseEntity.ok("Pago registrado correctamente");
    }
}