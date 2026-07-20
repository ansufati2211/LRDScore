package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.services.interfaces.IUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // CORRECCIÓN: Inyecta la sede desde el contexto
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios(TenantContext.getCurrentSede()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Usuario> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<Usuario> crearUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<String> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok("Usuario desactivado exitosamente.");
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("#id == authentication.principal.usuarioId")
    public ResponseEntity<String> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordDTO dto) {
        usuarioService.cambiarPassword(id, dto);
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }

    @PutMapping("/{id}/resetear-password")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<String> resetearPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        usuarioService.resetPassword(id, body.get("passwordNueva"));
        return ResponseEntity.ok("Contraseña reseteada exitosamente.");
    }
}