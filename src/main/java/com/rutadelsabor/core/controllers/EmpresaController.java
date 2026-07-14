package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.EmpresaRequestDTO;
import com.rutadelsabor.core.models.entities.Empresa;
import com.rutadelsabor.core.repositories.EmpresaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    public EmpresaController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    // 👇 ESTE ES EL ENDPOINT QUE FALTABA PARA QUE REACT PUEDA LEER LA CONFIGURACIÓN 👇
    @GetMapping("/mi-empresa")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Empresa> obtenerMiEmpresa() {
        Long empresaId = TenantContext.getCurrentTenant();
        return empresaRepository.findById(empresaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Empresa> registrarEmpresa(@RequestBody EmpresaRequestDTO request) {
        return new ResponseEntity<>(new Empresa(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Empresa> actualizarEmpresa(@PathVariable Long id, @RequestBody EmpresaRequestDTO request) {
        return ResponseEntity.ok(new Empresa());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> darDeBajaEmpresa(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}