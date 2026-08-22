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
import com.rutadelsabor.core.services.reportes.ExcelReportManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CajaServiceImpl implements ICajaService {

    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final ProductoRepository productoRepository;
    private final SseEmitterManager sseEmitterManager;
    private final ExcelReportManager excelReportManager;

    public CajaServiceImpl(CajaRepository cajaRepository,
                           UsuarioRepository usuarioRepository,
                           TransaccionPagoRepository transaccionPagoRepository,
                           ProductoRepository productoRepository,
                           SseEmitterManager sseEmitterManager,
                           ExcelReportManager excelReportManager) {
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.productoRepository = productoRepository;
        this.sseEmitterManager = sseEmitterManager;
        this.excelReportManager = excelReportManager;
    }

    @Override
    @Transactional
    public SesionCaja abrirCaja(Long cajeroId, BigDecimal montoInicial, Long sedeId) {
        cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .ifPresent(c -> { throw new ReglaNegocioException("El cajero ya tiene una sesion de caja abierta."); });

        Usuario cajero = usuarioRepository.findById(cajeroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajero no encontrado"));

        SesionCaja sesion = new SesionCaja();
        sesion.setCajero(cajero);
        sesion.setSedeId(TenantContext.resolverSedeEfectiva(sedeId));
        sesion.setMontoInicial(montoInicial);
        sesion.setEstado(EstadoCaja.ABIERTA);
        sesion.setFechaApertura(LocalDateTime.now());
        
        return cajaRepository.save(sesion);
    }

    @Override
    @Transactional
    public SesionCaja cerrarCaja(Long sesionCajaId, BigDecimal montoFinalDeclarado) {
        SesionCaja sesion = cajaRepository.findById(sesionCajaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesion de caja no encontrada"));
                
        if (sesion.getEstado() == EstadoCaja.CERRADA) {
            throw new ReglaNegocioException("La sesion de caja ya se encuentra cerrada.");
        }
        
        BigDecimal totalEfectivo = transaccionPagoRepository.sumarPorSesionYMetodo(sesionCajaId, MetodoPago.EFECTIVO);
        sesion.setMontoFinalCalculado(sesion.getMontoInicial().add(totalEfectivo != null ? totalEfectivo : BigDecimal.ZERO));
        sesion.setEstado(EstadoCaja.CERRADA);
        sesion.setFechaCierre(LocalDateTime.now());
        sesion.setMontoFinalDeclarado(montoFinalDeclarado);
        
        SesionCaja sesionCerrada = cajaRepository.save(sesion);

        List<Producto> agotados = productoRepository.findByEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_SERVICIO);
        if (!agotados.isEmpty()) {
            agotados.forEach(p -> p.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE));
            productoRepository.saveAll(agotados);
            sseEmitterManager.publicarTenant(TenantContext.getCurrentTenant(), "RESET_DISPONIBILIDAD", Map.of(
                    "count", agotados.size(),
                    "mensaje", "Productos restablecidos al cierre de caja"
            ));
        }
        return sesionCerrada;
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCaja obtenerCajaActivaPorCajero(Long cajeroId, Long sedeId) {
        return cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna sesion de caja abierta para este cajero."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarHistorialPorCajero(Long cajeroId, Long sedeId) {
        return cajaRepository.findByCajeroIdOrderByFechaAperturaDesc(cajeroId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> obtenerResumenCajaActiva(Long cajeroId, Long sedeId) {
        SesionCaja caja = cajaRepository.findByCajeroIdAndEstado(cajeroId, EstadoCaja.ABIERTA).orElse(null);
        Map<String, BigDecimal> resumen = new HashMap<>();
        if (caja == null) return resumen;

        resumen.put("EFECTIVO", transaccionPagoRepository.sumarPorSesionYMetodo(caja.getId(), MetodoPago.EFECTIVO));
        resumen.put("YAPE", transaccionPagoRepository.sumarPorSesionYMetodo(caja.getId(), MetodoPago.YAPE));
        resumen.put("PLIN", transaccionPagoRepository.sumarPorSesionYMetodo(caja.getId(), MetodoPago.PLIN));
        resumen.put("TARJETA", transaccionPagoRepository.sumarPorSesionYMetodo(caja.getId(), MetodoPago.TARJETA));

        return resumen;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarAuditoriaCajas(LocalDate inicio, LocalDate fin, Long sedeIdFiltro) {
        Long empresaId = TenantContext.getCurrentTenant();
        Long sedeId = TenantContext.getCurrentSede();
        LocalDateTime fechaInicio = inicio.atStartOfDay();
        LocalDateTime fechaFin = fin.atTime(LocalTime.MAX);

        if (sedeId != null) {
            return cajaRepository.findBySedeIdAndFechaAperturaBetween(sedeId, fechaInicio, fechaFin);
        } else if (sedeIdFiltro != null) {
            return cajaRepository.findBySedeIdAndFechaAperturaBetween(sedeIdFiltro, fechaInicio, fechaFin);
        } else {
            return cajaRepository.findByEmpresaIdAndFechaAperturaBetween(empresaId, fechaInicio, fechaFin);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarAuditoriaExcel(LocalDate inicio, LocalDate fin, Long sedeId) {
        List<SesionCaja> sesiones = listarAuditoriaCajas(inicio, fin, sedeId);
        return excelReportManager.generarReporteAuditoriaCajas(sesiones);
    }

    @Scheduled(cron = "59 59 23 * * ?")
    @Transactional
    public void forzarCierreCajasMedianoche() {
        List<SesionCaja> cajasAbiertas = cajaRepository.findByEstado(EstadoCaja.ABIERTA);

        for (SesionCaja sesion : cajasAbiertas) {
            BigDecimal totalEfectivo = transaccionPagoRepository.sumarPorSesionYMetodo(sesion.getId(), MetodoPago.EFECTIVO);
            BigDecimal esperado = sesion.getMontoInicial().add(totalEfectivo != null ? totalEfectivo : BigDecimal.ZERO);

            sesion.setMontoFinalCalculado(esperado);
            sesion.setMontoFinalDeclarado(esperado);
            sesion.setEstado(EstadoCaja.CERRADA);
            sesion.setFechaCierre(LocalDateTime.now());

            cajaRepository.save(sesion);

            List<Producto> agotados = productoRepository.findByEstadoDisponibilidad(EstadoDisponibilidad.AGOTADO_SERVICIO);
            if (!agotados.isEmpty()) {
                agotados.forEach(p -> p.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE));
                productoRepository.saveAll(agotados);
                sseEmitterManager.publicarTenant(sesion.getEmpresaId(), "RESET_DISPONIBILIDAD", Map.of(
                        "count", agotados.size(),
                        "mensaje", "Productos restablecidos al cierre automatico de medianoche"
                ));
            }
        }
    }
}