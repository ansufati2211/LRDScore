package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.CategoriaRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.InsumoRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.dto.request.ProductoRequestDTO;
import com.rutadelsabor.core.dto.response.InsumoFaltanteDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.exceptions.StockInsuficienteException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventarioServiceImpl implements IInventarioService {

    private static final String INSUMO_NO_ENCONTRADO = "Insumo no encontrado";
    private static final String TIPO_MOVIMIENTO_RESERVA = "RESERVA";

    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final PedidoDetalleRepository detalleRepository;

    public InventarioServiceImpl(CategoriaRepository categoriaRepository,
                                 InsumoRepository insumoRepository,
                                 ProductoRepository productoRepository,
                                 RecetaDetalleRepository recetaDetalleRepository,
                                 KardexMovimientoRepository kardexRepository,
                                 PedidoDetalleRepository detalleRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.kardexRepository = kardexRepository;
        this.detalleRepository = detalleRepository;
    }

    // --- CATEGORIAS ---

    @Override
    @Transactional
    public Categoria crearCategoria(Categoria categoria) { 
        return categoriaRepository.save(categoria); 
    }

    @Override
    @Transactional
    public Categoria actualizarCategoria(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            categoria.setNombre(dto.getNombre());
        }
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public void desactivarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setEstadoRegistro(false);
        categoriaRepository.save(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() { 
        return categoriaRepository.findAll(); 
    }

    // --- INSUMOS ---

    @Override
    @Transactional
    public Insumo crearInsumo(Insumo insumo) { 
        return insumoRepository.save(insumo); 
    }

    @Override
    @Transactional
    public Insumo actualizarInsumo(Long id, InsumoRequestDTO dto) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con ID: " + id));
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            insumo.setNombre(dto.getNombre());
        }
        if (dto.getUnidadMedida() != null && !dto.getUnidadMedida().isBlank()) {
            insumo.setUnidadMedida(dto.getUnidadMedida());
        }
        if (dto.getStockMinimo() != null) {
            insumo.setStockMinimo(dto.getStockMinimo());
        }
        return insumoRepository.save(insumo);
    }

    @Override
    @Transactional
    public void desactivarInsumo(Long id) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con ID: " + id));
        insumo.setEstadoRegistro(false);
        insumoRepository.save(insumo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Insumo> listarInsumos() { 
        return insumoRepository.findAll(); 
    }

    @Override
    @Transactional(readOnly = true)
    public List<Insumo> listarInsumosConStockBajo() { 
        return insumoRepository.findInsumosConStockBajo(); 
    }

    // --- PRODUCTOS ---

    @Override
    @Transactional
    public Producto crearProducto(Producto producto) { 
        return productoRepository.save(producto); 
    }

    @Override
    @Transactional
    public Producto actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            producto.setNombre(dto.getNombre());
        }
        if (dto.getPrecioVenta() != null) {
            producto.setPrecioVenta(dto.getPrecioVenta());
        }
        if (dto.getTagsBusqueda() != null) {
            producto.setTagsBusqueda(dto.getTagsBusqueda());
        }
        if (dto.getCategoriaId() != null) {
            Categoria cat = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
            producto.setCategoria(cat);
        }
        if (dto.getEsPreparado() != null) {
    producto.setEsPreparado(dto.getEsPreparado());
}
if (dto.getTiempoPreparacionMinutos() != null) {
    producto.setTiempoPreparacionMinutos(dto.getTiempoPreparacionMinutos());
}
        return productoRepository.save(producto);
    }

    @Override
    @Transactional
    public void desactivarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
        producto.setEstadoRegistro(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() { 
        return productoRepository.findAll(); 
    }

    @Override
    @Transactional
    public RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con el ID: " + productoId));
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con el ID: " + insumoId));

        RecetaDetalle receta = new RecetaDetalle();
        receta.setProducto(producto);
        receta.setInsumo(insumo);
        receta.setCantidadRequerida(cantidad);
        receta.setUnidadMedida(unidadMedida);

        return recetaDetalleRepository.save(receta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecetaDetalle> obtenerRecetaPorProducto(Long productoId) {
        return recetaDetalleRepository.findByProductoId(productoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KardexMovimiento> listarKardexPorInsumo(Long insumoId) {
        if (!insumoRepository.existsById(insumoId)) {
            throw new RecursoNoEncontradoException("Insumo no encontrado con ID: " + insumoId);
        }
        return kardexRepository.findByInsumoIdOrderByCreatedAtDesc(insumoId);
    }

    // --- LÓGICA DE KARDEX ---

    @Override
    @Transactional
    public void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usuario) {
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(INSUMO_NO_ENCONTRADO));

        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockPosterior = stockAnterior.add(dto.getCantidad());

        BigDecimal valorAnterior = stockAnterior.multiply(insumo.getCostoUnitario() != null ? insumo.getCostoUnitario() : BigDecimal.ZERO);
        BigDecimal valorNuevo = dto.getCantidad().multiply(dto.getCostoUnitario());
        BigDecimal nuevoCostoUnitario = valorAnterior.add(valorNuevo).divide(stockPosterior, 2, RoundingMode.HALF_UP);

        insumo.setStockActual(stockPosterior);
        insumo.setCostoUnitario(nuevoCostoUnitario);
        insumoRepository.save(insumo);

        registrarMovimientoKardex(insumo, "ENTRADA_COMPRA", dto.getCantidad(), stockAnterior, stockPosterior, nuevoCostoUnitario, usuario, dto.getObservacion());
    }

    @Override
    @Transactional
    public void registrarMerma(MermaRequestDTO dto, Usuario usuario) {
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(INSUMO_NO_ENCONTRADO));

        if (insumo.getStockActual().compareTo(dto.getCantidad()) < 0) {
            throw new ReglaNegocioException("Stock insuficiente para registrar merma");
        }

        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockPosterior = stockAnterior.subtract(dto.getCantidad());

        insumo.setStockActual(stockPosterior);
        insumoRepository.save(insumo);

        registrarMovimientoKardex(insumo, "SALIDA_MERMA", dto.getCantidad(), stockAnterior, stockPosterior, insumo.getCostoUnitario(), usuario, dto.getMotivo());
    }

    @Override
    @Transactional
    public void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usuario) {
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(INSUMO_NO_ENCONTRADO));

        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockPosterior = Boolean.TRUE.equals(dto.getEsPositivo())
                ? stockAnterior.add(dto.getCantidad())
                : stockAnterior.subtract(dto.getCantidad());

        if (stockPosterior.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el stock en negativo");
        }

        insumo.setStockActual(stockPosterior);
        insumoRepository.save(insumo);

        String tipo = Boolean.TRUE.equals(dto.getEsPositivo()) ? "ENTRADA_AJUSTE" : "SALIDA_AJUSTE";
        registrarMovimientoKardex(insumo, tipo, dto.getCantidad(), stockAnterior, stockPosterior, insumo.getCostoUnitario(), usuario, dto.getMotivo());
    }

    @SuppressWarnings("java:S107")
    private void registrarMovimientoKardex(Insumo insumo, String tipo, BigDecimal cantidad,
            BigDecimal ant, BigDecimal post, BigDecimal costo, Usuario usr, String obs) {
        KardexMovimiento kardex = new KardexMovimiento();
        kardex.setInsumo(insumo);
        kardex.setTipoMovimiento(tipo);
        kardex.setCantidad(cantidad);
        kardex.setStockAnterior(ant);
        kardex.setStockPosterior(post);
        kardex.setCostoUnitario(costo);
        kardex.setUsuario(usr);
        kardex.setObservacion(obs);
        kardexRepository.save(kardex);
    }

    // --- MÓDULO 3: RESERVA / CONSUMO / LIBERACIÓN ---

    @Override
    @Transactional
    public void reservarInsumosParaPedido(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, Insumo> insumosPorId = cargarInsumos(totalPorInsumo);
        List<InsumoFaltanteDTO> faltantes = validarDisponibilidadReserva(totalPorInsumo, insumosPorId);
        if (!faltantes.isEmpty()) {
            throw new StockInsuficienteException(faltantes);
        }
        registrarReservas(pedidoId, totalPorInsumo, insumosPorId);
    }

    @Override
    @Transactional
    public void liberarReservaDePedido(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        for (KardexMovimiento reserva : reservas) {
            Insumo insumo = reserva.getInsumo();
            BigDecimal cantidad = reserva.getCantidad();
            BigDecimal stockActual = insumo.getStockActual();

            insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
            insumoRepository.save(insumo);

            KardexMovimiento mov = new KardexMovimiento();
            mov.setInsumo(insumo);
            mov.setTipoMovimiento("LIBERACION_RESERVA");
            mov.setCantidad(cantidad);
            mov.setStockAnterior(stockActual);
            mov.setStockPosterior(stockActual); 
            mov.setCostoUnitario(insumo.getCostoUnitario());
            mov.setPedidoId(pedidoId);
            mov.setObservacion("Liberación por cancelación de pedido #" + pedidoId);
            kardexRepository.save(mov);
        }
    }

    @Override
    @Transactional
    public boolean convertirReservaAConsumo(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        boolean requiereRevision = false;

        for (KardexMovimiento reserva : reservas) {
            Insumo insumo = reserva.getInsumo();
            BigDecimal cantidad = reserva.getCantidad();

            if (insumo.getStockActual().compareTo(cantidad) < 0) {
                requiereRevision = true;
                insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
                insumoRepository.save(insumo);
                continue;
            }

            BigDecimal stockAnterior = insumo.getStockActual();
            BigDecimal stockPosterior = stockAnterior.subtract(cantidad);

            insumo.setStockActual(stockPosterior);
            insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
            insumoRepository.save(insumo);

            KardexMovimiento mov = new KardexMovimiento();
            mov.setInsumo(insumo);
            mov.setTipoMovimiento("CONSUMO_PRODUCCION");
            mov.setCantidad(cantidad);
            mov.setStockAnterior(stockAnterior);
            mov.setStockPosterior(stockPosterior);
            mov.setCostoUnitario(insumo.getCostoUnitario());
            mov.setPedidoId(pedidoId);
            mov.setObservacion("Consumo de producción para pedido #" + pedidoId);
            kardexRepository.save(mov);
        }

        return requiereRevision;
    }

    // --- MÓDULO 4: recipe-based para soporte multi-comanda ---

    @Override
    @Transactional
    public boolean convertirItemsAConsumo(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, Insumo> insumosPorId = cargarInsumos(totalPorInsumo);
        Map<Long, BigDecimal> costoUnitarioPorDetalle = calcularCostoUnitarioPorDetalle(detalles);
        
        boolean requiereRevision = consumirInsumos(pedidoId, totalPorInsumo, insumosPorId);
        persistirCostoUnitarioConsumido(detalles, costoUnitarioPorDetalle);

        return requiereRevision;
    }

    @Override
    @Transactional
    public void liberarReservaDeItems(Long pedidoId, List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, Insumo> insumosPorId = cargarInsumos(totalPorInsumo);
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            Insumo insumo = insumosPorId.get(entry.getKey());
            BigDecimal cantidad = entry.getValue();
            BigDecimal stockActual = insumo.getStockActual();

            insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
            insumoRepository.save(insumo);

            KardexMovimiento mov = new KardexMovimiento();
            mov.setInsumo(insumo);
            mov.setTipoMovimiento("LIBERACION_RESERVA");
            mov.setCantidad(cantidad);
            mov.setStockAnterior(stockActual);
            mov.setStockPosterior(stockActual);
            mov.setCostoUnitario(insumo.getCostoUnitario());
            mov.setPedidoId(pedidoId);
            mov.setObservacion("Liberación por cancelación de ítem en pedido #" + pedidoId);
            kardexRepository.save(mov);
        }
    }

    private Map<Long, BigDecimal> calcularTotalPorInsumo(List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> totalPorInsumo = new LinkedHashMap<>();
        for (PedidoDetalle detalle : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(detalle.getProducto().getId());
            if (receta.isEmpty()) {
                continue;
            }
            BigDecimal factor = new BigDecimal(detalle.getCantidad());
            for (RecetaDetalle rd : receta) {
                Insumo insumo = rd.getInsumo();
                totalPorInsumo.merge(insumo.getId(), rd.getCantidadRequerida().multiply(factor), BigDecimal::add);
            }
        }
        return totalPorInsumo;
    }

    private Map<Long, Insumo> cargarInsumos(Map<Long, BigDecimal> totalPorInsumo) {
        Map<Long, Insumo> insumosPorId = new LinkedHashMap<>();
        for (Long insumoId : totalPorInsumo.keySet()) {
            insumoRepository.findById(insumoId).ifPresent(insumo -> insumosPorId.put(insumoId, insumo));
        }
        return insumosPorId;
    }

    private List<InsumoFaltanteDTO> validarDisponibilidadReserva(Map<Long, BigDecimal> totalPorInsumo, Map<Long, Insumo> insumosPorId) {
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            Insumo insumo = insumosPorId.get(entry.getKey());
            BigDecimal necesario = entry.getValue();
            BigDecimal disponible = insumo.getStockActual().subtract(insumo.getStockReservado());
            if (disponible.compareTo(necesario) < 0) {
                faltantes.add(new InsumoFaltanteDTO(insumo.getNombre(), disponible.max(BigDecimal.ZERO), necesario));
            }
        }
        return faltantes;
    }

    private void registrarReservas(Long pedidoId, Map<Long, BigDecimal> totalPorInsumo, Map<Long, Insumo> insumosPorId) {
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            Insumo insumo = insumosPorId.get(entry.getKey());
            BigDecimal cantidad = entry.getValue();
            BigDecimal stockActual = insumo.getStockActual();

            insumo.setStockReservado(insumo.getStockReservado().add(cantidad));
            insumoRepository.save(insumo);

            KardexMovimiento mov = new KardexMovimiento();
            mov.setInsumo(insumo);
            mov.setTipoMovimiento(TIPO_MOVIMIENTO_RESERVA);
            mov.setCantidad(cantidad);
            mov.setStockAnterior(stockActual);
            mov.setStockPosterior(stockActual);
            mov.setCostoUnitario(insumo.getCostoUnitario());
            mov.setPedidoId(pedidoId);
            mov.setObservacion("Reserva para pedido #" + pedidoId);
            kardexRepository.save(mov);
        }
    }

    private Map<Long, BigDecimal> calcularCostoUnitarioPorDetalle(List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> costoUnitarioPorDetalle = new LinkedHashMap<>();
        for (PedidoDetalle detalle : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(detalle.getProducto().getId());
            if (receta.isEmpty()) {
                BigDecimal costoRef = !Boolean.TRUE.equals(detalle.getProducto().getEsPreparado())
                        && detalle.getProducto().getCostoReferencial() != null
                        ? detalle.getProducto().getCostoReferencial()
                        : BigDecimal.ZERO;
                costoUnitarioPorDetalle.put(detalle.getId(), costoRef);
                continue;
            }

            BigDecimal costoUnitarioItem = BigDecimal.ZERO;
            for (RecetaDetalle rd : receta) {
                BigDecimal costoInsumo = rd.getInsumo().getCostoUnitario() != null ? rd.getInsumo().getCostoUnitario() : BigDecimal.ZERO;
                costoUnitarioItem = costoUnitarioItem.add(rd.getCantidadRequerida().multiply(costoInsumo));
            }
            costoUnitarioPorDetalle.put(detalle.getId(), costoUnitarioItem);
        }
        return costoUnitarioPorDetalle;
    }

    private boolean consumirInsumos(Long pedidoId, Map<Long, BigDecimal> totalPorInsumo, Map<Long, Insumo> insumosPorId) {
        boolean requiereRevision = false;
        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            Insumo insumo = insumosPorId.get(entry.getKey());
            BigDecimal cantidad = entry.getValue();

            if (insumo.getStockActual().compareTo(cantidad) < 0) {
                requiereRevision = true;
                insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
                insumoRepository.save(insumo);
                continue;
            }

            BigDecimal stockAnterior = insumo.getStockActual();
            BigDecimal stockPosterior = stockAnterior.subtract(cantidad);
            insumo.setStockActual(stockPosterior);
            insumo.setStockReservado(insumo.getStockReservado().subtract(cantidad).max(BigDecimal.ZERO));
            insumoRepository.save(insumo);

            KardexMovimiento mov = new KardexMovimiento();
            mov.setInsumo(insumo);
            mov.setTipoMovimiento("CONSUMO_PRODUCCION");
            mov.setCantidad(cantidad);
            mov.setStockAnterior(stockAnterior);
            mov.setStockPosterior(stockPosterior);
            mov.setCostoUnitario(insumo.getCostoUnitario());
            mov.setPedidoId(pedidoId);
            mov.setObservacion("Consumo de producción para pedido #" + pedidoId);
            kardexRepository.save(mov);
        }
        return requiereRevision;
    }

    private void persistirCostoUnitarioConsumido(List<PedidoDetalle> detalles, Map<Long, BigDecimal> costoUnitarioPorDetalle) {
        for (PedidoDetalle detalle : detalles) {
            BigDecimal costo = costoUnitarioPorDetalle.get(detalle.getId());
            if (costo != null) {
                detalle.setCostoUnitarioConsumido(costo);
                detalleRepository.save(detalle);
            }
        }
    }

    @Override
@Transactional
public void activarCategoria(Long id) {
    Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
    categoria.setEstadoRegistro(true);
    categoriaRepository.save(categoria);
}

@Override
@Transactional
public void activarProducto(Long id) {
    Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
    producto.setEstadoRegistro(true);
    productoRepository.save(producto);
}
}

