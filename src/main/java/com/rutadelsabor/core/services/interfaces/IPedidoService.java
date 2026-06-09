package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.PagoRequestDTO;
import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;
import java.util.List;

public interface IPedidoService {
    Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo);
    Pedido obtenerPedido(Long id);
    void cancelarPedido(Long id);
    void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId);
    
    // NUEVA FIRMA: Para el listado de pedidos vivos en el restaurante
    List<Pedido> listarPedidosActivos();
}