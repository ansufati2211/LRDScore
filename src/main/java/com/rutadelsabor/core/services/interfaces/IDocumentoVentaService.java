package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AnularDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.request.EmitirDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoVentaResponseDTO;

import java.util.List;

public interface IDocumentoVentaService {

    // R7-2: NOTA_VENTA sin gate; R7-3: BOLETA/FACTURA gateadas en el servicio bajo FACTURACION
    DocumentoVentaResponseDTO emitir(EmitirDocumentoVentaRequestDTO dto);

    // E7-3: anulación como estado+motivo, no como borrado
    DocumentoVentaResponseDTO anular(Long documentoId, AnularDocumentoVentaRequestDTO dto);

    DocumentoVentaResponseDTO obtenerPorId(Long documentoId);
    List<DocumentoVentaResponseDTO> listarPorPedido(Long pedidoId);
    List<DocumentoVentaResponseDTO> listarPorDocumentoCobro(Long documentoCobroId);
}
