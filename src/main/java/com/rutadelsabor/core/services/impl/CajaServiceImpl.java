package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import com.rutadelsabor.core.repositories.CajaRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IAuditoriaService;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CajaServiceImpl implements ICajaService {

    // 1. Variables inmutables (final) sin @Autowired de campo (Resuelve java:S6813)
    private final IAuditoriaService auditoriaService;
    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;

    // 2. Inyección por Constructor: Garantiza alta cohesión y acoplamiento limpio
    public CajaServiceImpl(IAuditoriaService auditoriaService, 
                           CajaRepository cajaRepository, 
                           UsuarioRepository usuarioRepository) {
        this.auditoriaService = auditoriaService;
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial) {
        // Usamos ReglaNegocioException mapeada al código 400 Bad Request (Resuelve java:S112)
        cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .ifPresent(c -> { 
                    throw new ReglaNegocioException("El cajero ya tiene una sesión de caja abierta."); 
                });

        // Usamos RecursoNoEncontradoException mapeada al código 404 (Resuelve java:S112)
        Usuario cajero = usuarioRepository.findById(cajeroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajero no encontrado en el sistema."));

        SesionCaja sesion = new SesionCaja();
        sesion.setCajero(cajero);
        
        // Sincronizamos con el tipo LocalDateTime nativo (Resuelve fallos de compilación)
        sesion.setFechaApertura(LocalDateTime.now());
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de caja no encontrada."));

        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ReglaNegocioException("Esta caja ya fue cerrada anteriormente.");
        }

        // Sincronizamos con el tipo LocalDateTime nativo (Resuelve fallos de compilación)
        sesion.setFechaCierre(LocalDateTime.now());
        sesion.setMontoFinalDeclarado(montoFinalDeclarado);
        sesion.setEstado(EstadoCaja.CERRADA);

        // Registro estructurado en el Módulo de Auditoría de la Sede
        String detalleAuditoria = String.format("Caja cerrada. Monto esperado: S/%.2f | Monto declarado: S/%.2f", 
                                                sesion.getMontoFinalCalculado(), montoFinalDeclarado);
        
        auditoriaService.registrarAccion(
                sesion.getCajero().getId(), 
                sesion.getCajero().getEmpresaId(), 
                "CIERRE_CAJA", 
                "CAJA", 
                detalleAuditoria
        );

        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCaja obtenerCajaActiva(Long cajeroId) {
        return cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna caja abierta para este cajero actualmente."));
    }
}