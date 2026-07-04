package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.response.DashboardVentasDTO;
import com.rutadelsabor.core.dto.response.MargenVentasDTO;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.PedidoDetalle;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import com.rutadelsabor.core.repositories.PedidoDetalleRepository;
import com.rutadelsabor.core.repositories.VwDashboardVentasRepository;
import com.rutadelsabor.core.services.interfaces.IReporteService;
import com.rutadelsabor.core.services.reportes.ExcelReportManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteServiceImpl implements IReporteService {

    private final VwDashboardVentasRepository dashboardRepository;
    private final ExcelReportManager excelManager;
    private final PedidoDetalleRepository detalleRepository;

    public ReporteServiceImpl(VwDashboardVentasRepository dashboardRepository,
                              ExcelReportManager excelManager,
                              PedidoDetalleRepository detalleRepository) {
        this.dashboardRepository = dashboardRepository;
        this.excelManager = excelManager;
        this.detalleRepository = detalleRepository;
    }

    @Override
    public DashboardVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        List<VwDashboardVentas> ventas = dashboardRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin);

        DashboardVentasDTO dto = new DashboardVentasDTO();

        BigDecimal totalIngresos = ventas.stream()
                .map(VwDashboardVentas::getTotalIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalPedidos = ventas.stream()
                .mapToLong(VwDashboardVentas::getCantidadPedidos)
                .sum();

        List<DashboardVentasDTO.DetalleDiaDTO> detalle = ventas.stream().map(v -> {
            DashboardVentasDTO.DetalleDiaDTO d = new DashboardVentasDTO.DetalleDiaDTO();
            d.setFecha(v.getFecha().toString());
            d.setIngresos(v.getTotalIngresos());
            d.setPedidos(v.getCantidadPedidos());
            return d;
        }).toList();

        dto.setIngresosTotalesMensuales(totalIngresos);
        dto.setPedidosTotalesMensuales(totalPedidos);
        dto.setDetalleDiario(detalle);

        return dto;
    }

    @Override
    public byte[] exportarVentasExcel(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);
        List<VwDashboardVentas> ventas = dashboardRepository.findByFechaBetweenOrderByFechaAsc(inicio, fin);
        return excelManager.generarReporteVentas(ventas);
    }

    @Override
    public MargenVentasDTO obtenerMargenVentas(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        LocalDateTime inicioTs = inicio.atStartOfDay();
        LocalDateTime finTs = fin.atTime(23, 59, 59);

        List<PedidoDetalle> vendidos = detalleRepository.findDetallesConCostoPorPeriodo(
                EstadoPedido.PAGADO, inicioTs, finTs, EstadoItem.CANCELADO);

        List<PedidoDetalle> merma = detalleRepository.findDetallesMermaConCostoPorPeriodo(
                inicioTs, finTs, EstadoItem.CANCELADO);

        Map<Long, MargenVentasDTO.MargenProductoDTO> porProducto = new LinkedHashMap<>();
        Map<Long, MargenVentasDTO.MargenCategoriaDTO> porCategoria = new LinkedHashMap<>();
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        BigDecimal costoTotalVentas = BigDecimal.ZERO;

        for (PedidoDetalle pd : vendidos) {
            Producto producto = pd.getProducto();
            Categoria cat = producto.getCategoria();

            // E5-1: ingreso efectivo tras descuento proporcional del pedido
            BigDecimal subtotalItem = pd.getSubtotal();
            BigDecimal pedidoSubtotal = pd.getPedido().getSubtotal();
            BigDecimal pedidoTotal = pd.getPedido().getTotal();
            BigDecimal factorDescuento = pedidoSubtotal.compareTo(BigDecimal.ZERO) != 0
                    ? pedidoTotal.divide(pedidoSubtotal, 10, RoundingMode.HALF_UP)
                    : BigDecimal.ONE;
            BigDecimal ingresoEfectivo = subtotalItem.multiply(factorDescuento).setScale(2, RoundingMode.HALF_UP);

            BigDecimal costoItem = pd.getCostoUnitarioConsumido()
                    .multiply(new BigDecimal(pd.getCantidad()))
                    .setScale(4, RoundingMode.HALF_UP);

            ingresosTotales = ingresosTotales.add(ingresoEfectivo);
            costoTotalVentas = costoTotalVentas.add(costoItem);

            // R5-4: esEstimado = producto sin receta con costo_referencial configurado
            boolean esEstimado = !Boolean.TRUE.equals(producto.getEsPreparado())
                    && producto.getCostoReferencial() != null;

            // Desglose por producto (R5-3)
            porProducto.computeIfAbsent(producto.getId(), id -> {
                MargenVentasDTO.MargenProductoDTO p = new MargenVentasDTO.MargenProductoDTO();
                p.setProductoId(id);
                p.setProducto(producto.getNombre());
                p.setEsEstimado(esEstimado);
                p.setIngresos(BigDecimal.ZERO);
                p.setCostoVentas(BigDecimal.ZERO);
                return p;
            });
            MargenVentasDTO.MargenProductoDTO mp = porProducto.get(producto.getId());
            mp.setIngresos(mp.getIngresos().add(ingresoEfectivo));
            mp.setCostoVentas(mp.getCostoVentas().add(costoItem));

            // Desglose por categoría (R5-3)
            porCategoria.computeIfAbsent(cat.getId(), id -> {
                MargenVentasDTO.MargenCategoriaDTO c = new MargenVentasDTO.MargenCategoriaDTO();
                c.setCategoriaId(id);
                c.setCategoria(cat.getNombre());
                c.setIngresos(BigDecimal.ZERO);
                c.setCostoVentas(BigDecimal.ZERO);
                return c;
            });
            MargenVentasDTO.MargenCategoriaDTO mc = porCategoria.get(cat.getId());
            mc.setIngresos(mc.getIngresos().add(ingresoEfectivo));
            mc.setCostoVentas(mc.getCostoVentas().add(costoItem));
        }

        // E5-2: costo de merma (cancelados que ya consumieron inventario)
        BigDecimal costoMerma = BigDecimal.ZERO;
        for (PedidoDetalle pd : merma) {
            costoMerma = costoMerma.add(pd.getCostoUnitarioConsumido()
                    .multiply(new BigDecimal(pd.getCantidad()))
                    .setScale(4, RoundingMode.HALF_UP));
        }

        // Derivar utilidad y margen por producto
        List<MargenVentasDTO.MargenProductoDTO> desgloseProductos = new ArrayList<>(porProducto.values());
        desgloseProductos.forEach(p -> {
            BigDecimal ub = p.getIngresos().subtract(p.getCostoVentas());
            p.setUtilidadBruta(ub);
            p.setMargenPct(calcularMargenPct(p.getIngresos(), ub));
        });

        // Derivar utilidad y margen por categoría
        List<MargenVentasDTO.MargenCategoriaDTO> desgloseCategoria = new ArrayList<>(porCategoria.values());
        desgloseCategoria.forEach(c -> {
            BigDecimal ub = c.getIngresos().subtract(c.getCostoVentas());
            c.setUtilidadBruta(ub);
            c.setMargenPct(calcularMargenPct(c.getIngresos(), ub));
        });

        BigDecimal utilidadBruta = ingresosTotales.subtract(costoTotalVentas);
        BigDecimal margenBrutoPct = calcularMargenPct(ingresosTotales, utilidadBruta);

        MargenVentasDTO dto = new MargenVentasDTO();
        dto.setIngresosTotales(ingresosTotales);
        dto.setCostoVentas(costoTotalVentas);
        dto.setUtilidadBruta(utilidadBruta);
        dto.setMargenBrutoPct(margenBrutoPct);
        dto.setCostoMerma(costoMerma);
        dto.setDesglosePorProducto(desgloseProductos);
        dto.setDesglosePorCategoria(desgloseCategoria);

        return dto;
    }

    private BigDecimal calcularMargenPct(BigDecimal ingresos, BigDecimal utilidad) {
        if (ingresos.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return utilidad.divide(ingresos, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Las fechas de inicio y fin para la generación del reporte no pueden ser nulas.");
        }
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("Inconsistencia de rango: La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha de fin (" + fin + ").");
        }
    }
}
