package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import com.rutadelsabor.core.repositories.CajaRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class CajaServiceImpl implements ICajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial) {
        // 1. Regla de negocio: Validar si el cajero ya tiene una caja abierta
        cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .ifPresent(c -> { throw new RuntimeException("El cajero ya tiene una sesión de caja abierta."); });

        // 2. Buscar al cajero en la BD
        Usuario cajero = usuarioRepository.findById(cajeroId)
                .orElseThrow(() -> new RuntimeException("Cajero no encontrado en el sistema."));

        // 3. Inicializar la sesión
        SesionCaja sesion = new SesionCaja();
        sesion.setCajero(cajero);
        sesion.setFechaApertura(new Date());
        sesion.setMontoInicial(montoInicial);
        sesion.setEstado(EstadoCaja.ABIERTA);
        
        // Al inicio, el monto final calculado es igual al dinero base con el que abre
        sesion.setMontoFinalCalculado(montoInicial); 

        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional
    public SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado) {
        SesionCaja sesion = cajaRepository.findById(sesionCajaId)
                .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada."));

        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new RuntimeException("Esta caja ya fue cerrada anteriormente.");
        }

        sesion.setFechaCierre(new Date());
        sesion.setMontoFinalDeclarado(montoFinalDeclarado);
        sesion.setEstado(EstadoCaja.CERRADA);

        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCaja obtenerCajaActiva(Long cajeroId) {
        return cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("No hay ninguna caja abierta para este cajero actualmente."));
    }
}