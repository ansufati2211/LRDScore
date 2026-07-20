package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.ClienteRequestDTO;
import com.rutadelsabor.core.models.entities.Cliente;
import com.rutadelsabor.core.repositories.ClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO')")
public class ClienteController {
    
    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @PostMapping
    public ResponseEntity<Cliente> registrarCliente(@RequestBody ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombreRazonSocial(dto.getNombreRazonSocial());
        cliente.setTipoDocumento(dto.getTipoDocumento());
        cliente.setNumeroDocumento(dto.getNumeroDocumento());
        cliente.setDireccion(dto.getDireccion());
        cliente.setCorreo(dto.getCorreo());
        cliente.setTelefono(dto.getTelefono());
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    @GetMapping("/buscar/{documento}")
    public ResponseEntity<Cliente> buscarPorDocumento(@PathVariable String documento) {
        return clienteRepository.findByNumeroDocumento(documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<List<Cliente>> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }
}