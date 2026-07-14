package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
public class SedeController {

    private final SedeRepository sedeRepository;

    public SedeController(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Sede>> listarSedesPorEmpresa() {
        Long empresaId = TenantContext.getCurrentTenant();
        // Devolvemos tanto activas como inactivas para el panel de administración
        return ResponseEntity.ok(sedeRepository.findAll().stream()
                .filter(s -> s.getEmpresaId().equals(empresaId))
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Sede> crearSede(@RequestBody Sede sedeRequest) {
        sedeRequest.setEmpresaId(TenantContext.getCurrentTenant());
        sedeRequest.setEstadoRegistro(true);
        return ResponseEntity.ok(sedeRepository.save(sedeRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Sede> actualizarSede(@PathVariable Long id, @RequestBody Sede sedeRequest) {
        Sede sede = sedeRepository.findById(id).orElseThrow();
        if (sedeRequest.getNombre() != null) sede.setNombre(sedeRequest.getNombre());
        if (sedeRequest.getDireccion() != null) sede.setDireccion(sedeRequest.getDireccion());
        if (sedeRequest.getCodigoEstablecimiento() != null) sede.setCodigoEstablecimiento(sedeRequest.getCodigoEstablecimiento());
        return ResponseEntity.ok(sedeRepository.save(sede));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> inhabilitarSede(@PathVariable Long id) {
        Sede sede = sedeRepository.findById(id).orElseThrow();
        sede.setEstadoRegistro(false);
        sedeRepository.save(sede);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> activarSede(@PathVariable Long id) {
        Sede sede = sedeRepository.findById(id).orElseThrow();
        sede.setEstadoRegistro(true);
        sedeRepository.save(sede);
        return ResponseEntity.ok().build();
    }
}