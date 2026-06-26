package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.SesionCaja;
import java.math.BigDecimal;

public interface ICajaService {
    SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial);
    SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado);
    
    // El método que el controlador estaba buscando
    SesionCaja obtenerCajaActivaPorCajero(Long cajeroId); 
}