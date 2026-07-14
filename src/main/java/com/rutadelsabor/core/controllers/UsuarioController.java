package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 🚨 MODO DEBUG PARA CREAR USUARIO 🚨
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequestDTO dto) {
        try {
            return new ResponseEntity<>(usuarioService.crearUsuario(dto), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            String causa = e.getCause() != null ? e.getCause().getMessage() : "";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Fallo SQL: " + e.getMessage() + " | Detalles: " + causa));
        }
    }

    // 🚨 MODO DEBUG PARA ACTUALIZAR USUARIO 🚨
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequestDTO dto) {
        try {
            return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
        } catch (Exception e) {
            e.printStackTrace();
            String causa = e.getCause() != null ? e.getCause().getMessage() : "";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Fallo SQL: " + e.getMessage() + " | Detalles: " + causa));
        }
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA') or #id == authentication.principal.usuarioId")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id, @RequestBody CambiarPasswordDTO dto) {
        usuarioService.cambiarPassword(id, dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> resetearPasswordAdmin(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String nuevaPassword = payload.get("nuevaPassword");
        usuarioService.resetPassword(id, nuevaPassword);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> activarUsuario(@PathVariable Long id) {
        usuarioService.activarUsuario(id);
        return ResponseEntity.ok().build();
    }

@GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Usuario>> listarUsuarios(@RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(sedeId));
    }

    @GetMapping("/me")
    public ResponseEntity<Usuario> obtenerPerfil(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(usuarioService.obtenerUsuario(userDetails.getUsuarioId()));
    }
}