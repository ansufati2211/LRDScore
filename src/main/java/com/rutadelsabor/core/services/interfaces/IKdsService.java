package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.response.KdsCocinaDTO;
import com.rutadelsabor.core.dto.response.PorcionDisponibleDTO;

import java.util.List;

public interface IKdsService {
    List<KdsCocinaDTO> obtenerPedidosPendientes(Long sedeIdFiltro);
    void marcarPreparando(Long pedidoId, Long usuarioId);
    void marcarListo(Long pedidoId);
    
    void marcarAgotadoTemporal(Long productoId);
    void marcarAgotadoServicio(Long productoId);
    void revertirDisponible(Long productoId);
    List<PorcionDisponibleDTO> calcularPorcionesDisponibles(Long sedeIdFiltro);
}