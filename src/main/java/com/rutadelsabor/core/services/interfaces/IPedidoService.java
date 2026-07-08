package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AgregarItemsRequestDTO;
import com.rutadelsabor.core.dto.request.DocumentoCobroRequestDTO;
import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoCobroResponseDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IPedidoService {
    Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo);
    Pedido confirmarPedido(Long id);
    Pedido entregarPedido(Long id);
    void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId);
    List<PedidoActivoResponseDTO> listarPedidosActivos(Long sedeIdFiltro);
    List<PedidoActivoResponseDTO> listarHistorial(LocalDate inicio, LocalDate fin, Long sedeIdFiltro);
    Pedido cancelarPedido(Long id);
    Pedido aplicarDescuento(Long id, BigDecimal monto);
    Pedido obtenerPedido(Long id);
    
    void agregarItemsAPedido(Long pedidoId, AgregarItemsRequestDTO dto);
    void cancelarItem(Long pedidoId, Long detalleId, String motivo, boolean esGerente);
    DocumentoCobroResponseDTO crearDocumentoCobro(Long pedidoId, DocumentoCobroRequestDTO dto);
    DocumentoCobroResponseDTO pagarDocumentoCobro(Long documentoId, PagoRequestDTO pagoDTO, Long cajeroId);
    List<DocumentoCobroResponseDTO> listarDocumentosCobro(Long pedidoId);
}