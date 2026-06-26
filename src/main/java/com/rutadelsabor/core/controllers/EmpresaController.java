package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.EmpresaRequestDTO;
import com.rutadelsabor.core.models.entities.Empresa;
import com.rutadelsabor.core.repositories.EmpresaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    public EmpresaController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> obtenerEmpresa(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Empresa> actualizarEmpresa(
            @PathVariable Long id,
            @RequestBody EmpresaRequestDTO detalles) {
        return empresaRepository.findById(id).map(empresa -> {
            empresa.setNombreComercial(detalles.getNombreComercial());
            empresa.setRuc(detalles.getRuc());
            empresa.setDireccion(detalles.getDireccion());
            return ResponseEntity.ok(empresaRepository.save(empresa));
        }).orElse(ResponseEntity.notFound().build());
    }
}
