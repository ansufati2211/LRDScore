package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;

import java.math.BigDecimal;
import java.util.List;

public interface IPedidoService {
    Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo);
    Pedido obtenerPedido(Long id);
    void confirmarPedido(Long id);
    void aplicarDescuento(Long id, BigDecimal descuento);
    void entregarPedido(Long id);
    void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId);
    void cancelarPedido(Long id);
    List<PedidoActivoResponseDTO> listarPedidosActivos();
}