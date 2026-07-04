package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;
import com.rutadelsabor.core.models.entities.VwKdsCocina;

import java.util.List;

public interface IKdsService {
    List<VwKdsCocina> obtenerPedidosPendientes();
    void marcarPreparando(Long pedidoId, Long usuarioId);
    void marcarListo(Long pedidoId);

    // R6-1: AGOTADO_TEMPORAL — reversible desde cocina (E6-2)
    void marcarAgotadoTemporal(Long productoId);
    // R6-2: AGOTADO_SERVICIO — solo se revierte en cierre de caja (R6-3)
    void marcarAgotadoServicio(Long productoId);
    // E6-2: revertir AGOTADO_TEMPORAL → DISPONIBLE; rechaza AGOTADO_SERVICIO
    void revertirDisponible(Long productoId);
    // R6-5: porciones disponibles por receta para advertencia automática
    List<PorcionDisponibleDTO> calcularPorcionesDisponibles();
}
