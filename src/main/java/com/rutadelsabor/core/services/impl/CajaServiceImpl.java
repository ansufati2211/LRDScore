package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import com.rutadelsabor.core.repositories.CajaRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CajaServiceImpl implements ICajaService {

    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaServiceImpl(CajaRepository cajaRepository, UsuarioRepository usuarioRepository) {
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial) {
        cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .ifPresent(c -> { throw new ReglaNegocioException("El cajero ya tiene una sesión de caja abierta."); });

        Usuario cajero = usuarioRepository.findById(cajeroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajero no encontrado"));

        SesionCaja sesion = new SesionCaja();
        sesion.setCajero(cajero);
        sesion.setMontoInicial(montoInicial);
        sesion.setEstado(EstadoCaja.ABIERTA);
        sesion.setFechaApertura(LocalDateTime.now());

        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional
    public SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado) {
        SesionCaja sesion = cajaRepository.findById(sesionCajaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de caja no encontrada"));

        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ReglaNegocioException("La sesión de caja ya se encuentra cerrada.");
        }

        sesion.setEstado(EstadoCaja.CERRADA);
        sesion.setFechaCierre(LocalDateTime.now());
        sesion.setMontoFinalDeclarado(montoFinalDeclarado);

        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCaja obtenerCajaActivaPorCajero(Long cajeroId) {
        return cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna sesión de caja abierta para este cajero."));
    }
}