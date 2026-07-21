package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.SesionCaja;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ICajaService {
    SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial, Long sedeId);
    SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado);
    SesionCaja obtenerCajaActivaPorCajero(Long cajeroId, Long sedeId);
    List<SesionCaja> listarHistorialPorCajero(Long cajeroId, Long sedeId);

    Map<String, BigDecimal> obtenerResumenCajaActiva(Long cajeroId, Long sedeId);
}