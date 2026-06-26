package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import java.util.List;

public interface IKdsService {
    List<VwKdsCocina> obtenerPedidosPendientes();
    void marcarPreparando(Long pedidoId, Long usuarioId); // Modificado para el SP
    void marcarListo(Long pedidoId);
}