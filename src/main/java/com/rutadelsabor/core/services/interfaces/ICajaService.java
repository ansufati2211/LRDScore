package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.SesionCaja;

import java.math.BigDecimal;
import java.util.List;

public interface ICajaService {
    SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial);
    SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado);
    SesionCaja obtenerCajaActivaPorCajero(Long cajeroId);
    List<SesionCaja> listarHistorialPorCajero(Long cajeroId);
}