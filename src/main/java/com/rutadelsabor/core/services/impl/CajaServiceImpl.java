package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
import com.rutadelsabor.core.models.enums.MetodoPago;
import com.rutadelsabor.core.repositories.CajaRepository;
import com.rutadelsabor.core.repositories.ProductoRepository;
import com.rutadelsabor.core.repositories.TransaccionPagoRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.ICajaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CajaServiceImpl implements ICajaService {

    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final ProductoRepository productoRepository;
    private final SseEmitterManager sseEmitterManager;

    public CajaServiceImpl(CajaRepository cajaRepository,
                           UsuarioRepository usuarioRepository,
                           TransaccionPagoRepository transaccionPagoRepository,
                           ProductoRepository productoRepository,
                           SseEmitterManager sseEmitterManager) {
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.productoRepository = productoRepository;
        this.sseEmitterManager = sseEmitterManager;
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
        sesion.setFechaApertura(LocalDateTime.now(java.time.Clock.systemDefaultZone()));

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

        // montoFinalCalculado = efectivo inicial + suma de todos los pagos en EFECTIVO de la sesión
        // (YAPE, TARJETA y PLIN no entran a la caja física)
        BigDecimal totalEfectivo = transaccionPagoRepository.sumarPorSesionYMetodo(sesionCajaId, MetodoPago.EFECTIVO);
        sesion.setMontoFinalCalculado(sesion.getMontoInicial().add(totalEfectivo));

        sesion.setEstado(EstadoCaja.CERRADA);
        sesion.setFechaCierre(LocalDateTime.now(java.time.Clock.systemDefaultZone()));
        sesion.setMontoFinalDeclarado(montoFinalDeclarado);

        SesionCaja sesionCerrada = cajaRepository.save(sesion);

        // R6-3/E6-3: reset masivo de AGOTADO_SERVICIO → DISPONIBLE al cerrar caja
        List<Producto> agotados = productoRepository.findByEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_SERVICIO);
        if (!agotados.isEmpty()) {
            agotados.forEach(p -> p.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE));
            productoRepository.saveAll(agotados);
            sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "RESET_DISPONIBILIDAD", Map.of(
                    "count", agotados.size(),
                    "mensaje", "Productos AGOTADO_SERVICIO restablecidos al cierre de caja"
            ));
        }

        return sesionCerrada;
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCaja obtenerCajaActivaPorCajero(Long cajeroId) {
        return cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna sesión de caja abierta para este cajero."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarHistorialPorCajero(Long cajeroId) {
        return cajaRepository.findByCajeroIdOrderByFechaAperturaDesc(cajeroId);
    }
}