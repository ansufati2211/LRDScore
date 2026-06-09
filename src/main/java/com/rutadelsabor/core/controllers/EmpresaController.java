package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.EmpresaRequestDTO;
import com.rutadelsabor.core.models.entities.Empresa;
import com.rutadelsabor.core.repositories.EmpresaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    // 1. Inyección por Constructor (Resuelve java:S6813)
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
    // 2. Usamos EmpresaRequestDTO para proteger la base de datos (Resuelve java:S4684)
    public ResponseEntity<Empresa> actualizarEmpresa(
            @PathVariable Long id, 
            @RequestBody EmpresaRequestDTO detalles) {
        
        return empresaRepository.findById(id).map(empresa -> {
            // Solo actualizamos los campos seguros que vienen en el DTO
            empresa.setNombreComercial(detalles.getNombreComercial());
            empresa.setRuc(detalles.getRuc());
            empresa.setDireccion(detalles.getDireccion()); 
            
            return ResponseEntity.ok(empresaRepository.save(empresa));
        }).orElse(ResponseEntity.notFound().build());
    }
}