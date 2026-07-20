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

    @GetMapping("/mi-empresa")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Empresa> obtenerMiEmpresa() {
        Long empresaId = TenantContext.getCurrentTenant();
        return empresaRepository.findById(empresaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Empresa> registrarEmpresa(@RequestBody EmpresaRequestDTO request) {
        Empresa empresa = new Empresa();
        empresa.setNombreComercial(request.getNombreComercial());
        empresa.setRuc(request.getRuc());
        empresa.setDireccion(request.getDireccion());
        empresa.setEstadoRegistro(true);
        return new ResponseEntity<>(empresaRepository.save(empresa), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Empresa> actualizarEmpresa(@PathVariable Long id, @RequestBody EmpresaRequestDTO request) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long objetivo = (tenantId != null) ? tenantId : id;

        return empresaRepository.findById(objetivo)
                .map(empresa -> {
                    if (request.getNombreComercial() != null) empresa.setNombreComercial(request.getNombreComercial());
                    if (request.getRuc() != null) empresa.setRuc(request.getRuc());
                    if (request.getDireccion() != null) empresa.setDireccion(request.getDireccion());
                    return ResponseEntity.ok(empresaRepository.save(empresa));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> darDeBajaEmpresa(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(empresa -> {
                    empresa.setEstadoRegistro(false);
                    empresaRepository.save(empresa);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}