package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AnularDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.request.EmitirDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoVentaResponseDTO;
import java.util.List;

public interface IDocumentoVentaService {
    DocumentoVentaResponseDTO emitir(EmitirDocumentoVentaRequestDTO dto);
    DocumentoVentaResponseDTO anular(Long documentoId, AnularDocumentoVentaRequestDTO dto);
    DocumentoVentaResponseDTO obtenerPorId(Long documentoId);
    List<DocumentoVentaResponseDTO> listarPorPedido(Long pedidoId);
    List<DocumentoVentaResponseDTO> listarPorDocumentoCobro(Long documentoCobroId);
}