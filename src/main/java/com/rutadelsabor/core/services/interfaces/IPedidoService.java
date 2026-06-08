package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.PedidoRequestDTO;
import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.Usuario;

public interface IPedidoService {
    Pedido crearPedido(PedidoRequestDTO dto, Usuario mozo);
    Pedido obtenerPedido(Long id);
    void cancelarPedido(Long id);
}