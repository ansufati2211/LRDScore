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

    // R7-2: NOTA_VENTA disponible en plan básico; BOLETA/FACTURA gateadas en servicio (E7-1)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<DocumentoVentaResponseDTO> emitir(@RequestBody EmitirDocumentoVentaRequestDTO dto) {
        return ResponseEntity.ok(documentoVentaService.emitir(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<DocumentoVentaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoVentaService.obtenerPorId(id));
    }

    // Listar comprobantes emitidos para un pedido
    @GetMapping("/por-pedido/{pedidoId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoVentaResponseDTO>> listarPorPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(documentoVentaService.listarPorPedido(pedidoId));
    }

    // Listar comprobantes emitidos para un documento de cobro (split billing)
    @GetMapping("/por-documento-cobro/{documentoCobroId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO')")
    public ResponseEntity<List<DocumentoVentaResponseDTO>> listarPorDocumentoCobro(@PathVariable Long documentoCobroId) {
        return ResponseEntity.ok(documentoVentaService.listarPorDocumentoCobro(documentoCobroId));
    }

    // E7-3: anulación como estado+motivo — solo GERENTE y SUPER_ADMIN
    @PutMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<DocumentoVentaResponseDTO> anular(@PathVariable Long id,
                                                             @RequestBody AnularDocumentoVentaRequestDTO dto) {
        return ResponseEntity.ok(documentoVentaService.anular(id, dto));
    }
}
