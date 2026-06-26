package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final IInventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(IInventarioService inventarioService, UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarEntrada(@RequestBody EntradaAlmacenRequestDTO dto, Authentication auth) {
        Usuario usr = obtenerUsuarioAuth(auth);
        inventarioService.registrarEntrada(dto, usr);
        return ResponseEntity.ok("Entrada registrada y Kardex actualizado (Promedio Ponderado recalculado).");
    }

    @PostMapping("/mermas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarMerma(@RequestBody MermaRequestDTO dto, Authentication auth) {
        Usuario usr = obtenerUsuarioAuth(auth);
        inventarioService.registrarMerma(dto, usr);
        return ResponseEntity.ok("Merma registrada y stock descontado exitosamente.");
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarAjuste(@RequestBody AjusteInventarioRequestDTO dto, Authentication auth) {
        Usuario usr = obtenerUsuarioAuth(auth);
        inventarioService.registrarAjuste(dto, usr);
        return ResponseEntity.ok("Ajuste de inventario aplicado exitosamente.");
    }

    private Usuario obtenerUsuarioAuth(Authentication auth) {
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado en el sistema"));
    }
}
