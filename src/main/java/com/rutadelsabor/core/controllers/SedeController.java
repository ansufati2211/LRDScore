package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(sedeRepository.findByEmpresaIdAndEstadoRegistroTrue(empresaId));
    }
}