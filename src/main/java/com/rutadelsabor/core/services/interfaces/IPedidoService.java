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
    
    // Nuevas firmas para el control de estados del Mozo
    void confirmarPedido(Long id); // Pasa de BORRADOR a RECIBIDO
    void entregarPedido(Long id);  // Pasa de LISTO a ENTREGADO
    
    void procesarPago(Long pedidoId, PagoRequestDTO pagoDTO, Long cajeroId);
    List<Pedido> listarPedidosActivos();
}