package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.dto.response.PedidoActivoResponseDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import java.util.List;

public interface IPedidoService {
    Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo);
    Pedido obtenerPedido(Long id);
    void cancelarPedido(Long id);
    void confirmarPedido(Long id);
    void entregarPedido(Long id);
    void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId);
    List<PedidoActivoResponseDTO> listarPedidosActivos();
}