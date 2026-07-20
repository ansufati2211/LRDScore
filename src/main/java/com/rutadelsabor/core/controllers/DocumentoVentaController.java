package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.AnularDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.request.EmitirDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoVentaResponseDTO;
import com.rutadelsabor.core.services.interfaces.IDocumentoVentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/documentos-venta")
public class DocumentoVentaController {
    
    private final IDocumentoVentaService documentoVentaService;

    public DocumentoVentaController(IDocumentoVentaService documentoVentaService) {
        this.documentoVentaService = documentoVentaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoVentaResponseDTO> emitir(@RequestBody EmitirDocumentoVentaRequestDTO dto) {
        return ResponseEntity.ok(documentoVentaService.emitir(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<DocumentoVentaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoVentaService.obtenerPorId(id));
    }

    @GetMapping("/por-pedido/{pedidoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoVentaResponseDTO>> listarPorPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(documentoVentaService.listarPorPedido(pedidoId));
    }

    @GetMapping("/por-documento-cobro/{documentoCobroId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoVentaResponseDTO>> listarPorDocumentoCobro(@PathVariable Long documentoCobroId) {
        return ResponseEntity.ok(documentoVentaService.listarPorDocumentoCobro(documentoCobroId));
    }

    @PutMapping("/{id}/anular")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<DocumentoVentaResponseDTO> anular(@PathVariable Long id,
                                                             @RequestBody AnularDocumentoVentaRequestDTO dto) {
        return ResponseEntity.ok(documentoVentaService.anular(id, dto));
    }
}