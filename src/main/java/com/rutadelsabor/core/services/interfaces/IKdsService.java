package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import java.util.List;

public interface IKdsService {
    List<VwKdsCocina> obtenerPedidosPendientes();
    void marcarPreparando(Long pedidoId);
    void marcarListo(Long pedidoId);
}