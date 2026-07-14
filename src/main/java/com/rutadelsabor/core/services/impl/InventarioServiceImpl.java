package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;
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
import java.util.stream.Collectors;

@Service
public class InventarioServiceImpl implements IInventarioService {

    private static final String TIPO_MOVIMIENTO_RESERVA = "RESERVA";
    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final InsumoSedeRepository insumoSedeRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final PedidoDetalleRepository detalleRepository;
    private final SedeRepository sedeRepository;

    public InventarioServiceImpl(CategoriaRepository categoriaRepository, InsumoRepository insumoRepository,
                                 InsumoSedeRepository insumoSedeRepository, ProductoRepository productoRepository,
                                 RecetaDetalleRepository recetaDetalleRepository, KardexMovimientoRepository kardexRepository,
                                 PedidoDetalleRepository detalleRepository, SedeRepository sedeRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
        this.insumoSedeRepository = insumoSedeRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.kardexRepository = kardexRepository;
        this.detalleRepository = detalleRepository;
        this.sedeRepository = sedeRepository;
    }

    private record DatosKardex(
            Long sedeId,
            Insumo insumo, String tipoMovimiento, BigDecimal cantidad,
            BigDecimal stockAnterior, BigDecimal stockPosterior,
            BigDecimal costoUnitario, Usuario usuario,
            String observacion, Long pedidoId
    ) {}

    private void validarSedeDeEmpresa(Long sedeIdFiltro) {
        Sede sede = sedeRepository.findById(sedeIdFiltro)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        if (!sede.getEmpresaId().equals(TenantContext.getCurrentTenant())) {
            throw new ReglaNegocioException("La sede indicada no pertenece a su empresa.");
        }
    }

    @Override @Transactional public Categoria crearCategoria(Categoria c) { return categoriaRepository.save(c); }
    @Override @Transactional public Categoria actualizarCategoria(Long id, CategoriaRequestDTO dto) {
        Categoria c = categoriaRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) c.setNombre(dto.getNombre());
        return categoriaRepository.save(c);
    }
    @Override @Transactional public void desactivarCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id).orElseThrow(); c.setEstadoRegistro(false); categoriaRepository.save(c);
    }
    @Override @Transactional public void activarCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id).orElseThrow(); c.setEstadoRegistro(true); categoriaRepository.save(c);
    }
    @Override @Transactional(readOnly = true) public List<Categoria> listarCategorias() { return categoriaRepository.findAll(); }

    @Override @Transactional public Producto crearProducto(Producto p) { return productoRepository.save(p); }
    @Override @Transactional public Producto actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto p = productoRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) p.setNombre(dto.getNombre());
        if (dto.getPrecioVenta() != null) p.setPrecioVenta(dto.getPrecioVenta());
        if (dto.getTagsBusqueda() != null) p.setTagsBusqueda(dto.getTagsBusqueda());
        if (dto.getCategoriaId() != null) p.setCategoria(categoriaRepository.findById(dto.getCategoriaId()).orElseThrow());
        if (dto.getEsPreparado() != null) p.setEsPreparado(dto.getEsPreparado());
        if (dto.getTiempoPreparacionMinutos() != null) p.setTiempoPreparacionMinutos(dto.getTiempoPreparacionMinutos());
        return productoRepository.save(p);
    }
    @Override @Transactional public void desactivarProducto(Long id) { Producto p = productoRepository.findById(id).orElseThrow(); p.setEstadoRegistro(false); productoRepository.save(p); }
    @Override @Transactional public void activarProducto(Long id) { Producto p = productoRepository.findById(id).orElseThrow(); p.setEstadoRegistro(true); productoRepository.save(p); }
    @Override @Transactional(readOnly = true) public List<Producto> listarProductos() { return productoRepository.findAll(); }

    @Override
    @Transactional
    public RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida) {
        Producto p = productoRepository.findById(productoId).orElseThrow();
        Insumo i = insumoRepository.findById(insumoId).orElseThrow();
        RecetaDetalle r = new RecetaDetalle(); r.setProducto(p); r.setInsumo(i); r.setCantidadRequerida(cantidad); r.setUnidadMedida(unidadMedida);
        return recetaDetalleRepository.save(r);
    }

    @Override @Transactional(readOnly = true) public List<RecetaDetalle> obtenerRecetaPorProducto(Long pId) { return recetaDetalleRepository.findByProductoId(pId); }

    @Override
    @Transactional
    public void actualizarRecetaCompleta(Long productoId, Map<Long, BigDecimal> insumosYCantidades) {
        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
        List<RecetaDetalle> actuales = recetaDetalleRepository.findByProductoId(productoId);
        if (!actuales.isEmpty()) {
            recetaDetalleRepository.deleteAllInBatch(actuales);
        }
        List<RecetaDetalle> nuevos = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : insumosYCantidades.entrySet()) {
            Insumo i = insumoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + entry.getKey()));
            RecetaDetalle r = new RecetaDetalle();
            r.setProducto(p);
            r.setInsumo(i);
            r.setCantidadRequerida(entry.getValue());
            r.setUnidadMedida(i.getUnidadMedida());
            r.setEmpresaId(TenantContext.getCurrentTenant());
            nuevos.add(r);
        }
        recetaDetalleRepository.saveAll(nuevos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerRecetaFormateada(Long productoId) {
        return recetaDetalleRepository.findByProductoId(productoId).stream()
            .filter(r -> r.getInsumo() != null)
            .map(r -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("insumoId", r.getInsumo().getId());
                map.put("cantidadUsada", r.getCantidadRequerida());
                return map;
            }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarInsumosConCosto(Long sedeId) {
        return insumoRepository.findAll().stream().map(i -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", i.getId());
            map.put("nombre", i.getNombre());
            map.put("unidadMedida", i.getUnidadMedida());
            map.put("estadoRegistro", i.getEstadoRegistro() != null ? i.getEstadoRegistro() : true);
            
            BigDecimal costo = BigDecimal.ZERO;
            BigDecimal stockActual = BigDecimal.ZERO;
            BigDecimal stockMinimo = BigDecimal.ZERO;
            BigDecimal stockReservado = BigDecimal.ZERO;
            if (sedeId != null) {
                insumoSedeRepository.findBySedeIdAndInsumoId(sedeId, i.getId())
                    .ifPresent(is -> {
                        map.put("costoUnitario", is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO);
                        map.put("stockActual", is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO);
                        map.put("stockMinimo", is.getStockMinimo() != null ? is.getStockMinimo() : BigDecimal.ZERO);
                        map.put("stockReservado", is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO);
                    });
            }
            map.putIfAbsent("costoUnitario", costo);
            map.putIfAbsent("stockActual", stockActual);
            map.putIfAbsent("stockMinimo", stockMinimo);
            map.putIfAbsent("stockReservado", stockReservado);
            return map;
        }).collect(Collectors.toList());
    }

    @Override @Transactional public Insumo crearInsumo(Insumo i) { return insumoRepository.save(i); }
    @Override @Transactional public Insumo actualizarInsumo(Long id, InsumoRequestDTO dto) {
        Insumo i = insumoRepository.findById(id).orElseThrow();
        if (dto.getNombre() != null) i.setNombre(dto.getNombre());
        if (dto.getUnidadMedida() != null) i.setUnidadMedida(dto.getUnidadMedida());
        return insumoRepository.save(i);
    }
    @Override @Transactional public void desactivarInsumo(Long id) { Insumo i = insumoRepository.findById(id).orElseThrow(); i.setEstadoRegistro(false); insumoRepository.save(i); }
    @Override @Transactional(readOnly = true) public List<Insumo> listarInsumos() { return insumoRepository.findAll(); }
    @Override @Transactional public void activarInsumo(Long id) {
        Insumo i = insumoRepository.findById(id).orElseThrow();
        i.setEstadoRegistro(true);
        insumoRepository.save(i);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoBajoStockDTO> listarInsumosConStockBajo(Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        List<InsumoSede> insumosSede;
        if (sedeId != null) {
            insumosSede = insumoSedeRepository.findInsumosConStockBajoPorSede(sedeId);
        } else {
            if (sedeIdFiltro != null) {
                validarSedeDeEmpresa(sedeIdFiltro);
                insumosSede = insumoSedeRepository.findInsumosConStockBajoPorSede(sedeIdFiltro);
            } else {
                insumosSede = insumoSedeRepository.findInsumosConStockBajo();
            }
        }
        return insumosSede.stream().map(is -> {
            InsumoBajoStockDTO dto = new InsumoBajoStockDTO();
            dto.setInsumoId(is.getInsumo().getId());
            dto.setNombre(is.getInsumo().getNombre());
            dto.setUnidadMedida(is.getInsumo().getUnidadMedida());
            dto.setSedeId(is.getSedeId());
            dto.setStockActual(is.getStockActual());
            dto.setStockMinimo(is.getStockMinimo());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KardexMovimiento> listarKardexPorInsumo(Long insumoId, Long sedeIdFiltro) {
        Long sedeId = TenantContext.getCurrentSede();
        if (sedeId != null) {
            return kardexRepository.findBySedeIdAndInsumoIdOrderByCreatedAtDesc(sedeId, insumoId);
        } else {
            if (sedeIdFiltro != null) {
                validarSedeDeEmpresa(sedeIdFiltro);
                return kardexRepository.findBySedeIdAndInsumoIdOrderByCreatedAtDesc(sedeIdFiltro, insumoId);
            } else {
                return kardexRepository.findByInsumoIdOrderByCreatedAtDesc(insumoId);
            }
        }
    }

    private InsumoSede obtenerInventarioFisico(Long sedeEfectiva, Long insumoId) {
        return insumoSedeRepository.findBySedeIdAndInsumoId(sedeEfectiva, insumoId)
                .orElseGet(() -> {
                    Insumo insumoMaestro = insumoRepository.findById(insumoId)
                            .orElseThrow(() -> new RecursoNoEncontradoException("El insumo maestro no existe."));
                    InsumoSede nuevoIs = new InsumoSede();
                    nuevoIs.setInsumoId(insumoId);
                    nuevoIs.setSedeId(sedeEfectiva);
                    nuevoIs.setEmpresaId(TenantContext.getCurrentTenant());
                    nuevoIs.setStockActual(BigDecimal.ZERO);
                    nuevoIs.setStockMinimo(BigDecimal.ZERO);
                    nuevoIs.setStockReservado(BigDecimal.ZERO);
                    nuevoIs.setCostoUnitario(BigDecimal.ZERO);
                    nuevoIs.setInsumo(insumoMaestro);
                    return insumoSedeRepository.save(nuevoIs);
                });
    }

    @Override @Transactional
    public void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usr) {
        Long sedeEfectiva = TenantContext.resolverSedeEfectiva(dto.getSedeId());
        InsumoSede is = obtenerInventarioFisico(sedeEfectiva, dto.getInsumoId());
        BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
        BigDecimal post = ant.add(dto.getCantidad());
        BigDecimal valAnt = ant.multiply(is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO);
        BigDecimal valNuevo = dto.getCantidad().multiply(dto.getCostoUnitario());
        BigDecimal nuevoCosto = post.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : valAnt.add(valNuevo).divide(post, 2, RoundingMode.HALF_UP);
        is.setStockActual(post);
        is.setCostoUnitario(nuevoCosto);
        insumoSedeRepository.save(is);
        registrarMovimientoKardex(new DatosKardex(sedeEfectiva, is.getInsumo(), "ENTRADA_COMPRA", dto.getCantidad(), ant, post, nuevoCosto, usr, dto.getObservacion(), null));
    }

    @Override @Transactional
    public void registrarMerma(MermaRequestDTO dto, Usuario usr) {
        Long sedeEfectiva = TenantContext.resolverSedeEfectiva(dto.getSedeId());
        InsumoSede is = obtenerInventarioFisico(sedeEfectiva, dto.getInsumoId());
        BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
        if (ant.compareTo(dto.getCantidad()) < 0) throw new ReglaNegocioException("Stock físico insuficiente.");
        BigDecimal post = ant.subtract(dto.getCantidad());
        is.setStockActual(post);
        insumoSedeRepository.save(is);
        registrarMovimientoKardex(new DatosKardex(sedeEfectiva, is.getInsumo(), "SALIDA_MERMA", dto.getCantidad(), ant, post, is.getCostoUnitario(), usr, dto.getMotivo(), null));
    }

    @Override @Transactional
    public void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usr) {
        Long sedeEfectiva = TenantContext.resolverSedeEfectiva(dto.getSedeId());
        InsumoSede is = obtenerInventarioFisico(sedeEfectiva, dto.getInsumoId());
        BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
        BigDecimal post = Boolean.TRUE.equals(dto.getEsPositivo()) ? ant.add(dto.getCantidad()) : ant.subtract(dto.getCantidad());
        if (post.compareTo(BigDecimal.ZERO) < 0) throw new ReglaNegocioException("Stock negativo.");
        is.setStockActual(post);
        insumoSedeRepository.save(is);
        String tipo = Boolean.TRUE.equals(dto.getEsPositivo()) ? "ENTRADA_AJUSTE" : "SALIDA_AJUSTE";
        registrarMovimientoKardex(new DatosKardex(sedeEfectiva, is.getInsumo(), tipo, dto.getCantidad(), ant, post, is.getCostoUnitario(), usr, dto.getMotivo(), null));
    }

    private void registrarMovimientoKardex(DatosKardex req) {
        KardexMovimiento k = new KardexMovimiento();
        k.setEmpresaId(TenantContext.getCurrentTenant());
        k.setSedeId(req.sedeId());
        k.setInsumo(req.insumo()); 
        k.setTipoMovimiento(req.tipoMovimiento()); 
        k.setCantidad(req.cantidad());
        k.setStockAnterior(req.stockAnterior() != null ? req.stockAnterior() : BigDecimal.ZERO); 
        k.setStockPosterior(req.stockPosterior() != null ? req.stockPosterior() : BigDecimal.ZERO); 
        k.setCostoUnitario(req.costoUnitario() != null ? req.costoUnitario() : BigDecimal.ZERO);
        k.setUsuario(req.usuario()); 
        k.setObservacion(req.observacion()); 
        k.setPedidoId(req.pedidoId());
        kardexRepository.save(k);
    }

    // 🔥 FIX 500: PASAMOS SEDE_ID DIRECTAMENTE DESDE EL PEDIDO
    @Override @Transactional
    public void reservarInsumosParaPedido(Long pedidoId, List<PedidoDetalle> detalles) {
        if (detalles.isEmpty()) return;
        Long sedeIdDelPedido = detalles.get(0).getPedido().getSedeId();

        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo, sedeIdDelPedido);
        List<InsumoFaltanteDTO> faltantes = validarDisponibilidadReserva(totalPorInsumo, insumosPorId);
        
        if (!faltantes.isEmpty()) throw new StockInsuficienteException(faltantes);
                 
        Usuario operador = detalles.get(0).getPedido().getMozo();

        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            InsumoSede is = insumosPorId.get(entry.getKey());
            BigDecimal cant = entry.getValue();
            
            BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;
            
            is.setStockReservado(stockRes.add(cant));
            insumoSedeRepository.save(is);
            
            BigDecimal costoUnitario = is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO;
            registrarMovimientoKardex(new DatosKardex(is.getSedeId(), is.getInsumo(), TIPO_MOVIMIENTO_RESERVA, cant, ant, ant, costoUnitario, operador, "Reserva pedido #" + pedidoId, pedidoId));
        }
    }

    @Override @Transactional
    public void liberarReservaDePedido(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        for (KardexMovimiento r : reservas) {
            InsumoSede is = obtenerInventarioFisico(r.getSedeId(), r.getInsumo().getId());
            BigDecimal cant = r.getCantidad();
            
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;
            is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            BigDecimal actual = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal costoUnitario = is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO;
            registrarMovimientoKardex(new DatosKardex(is.getSedeId(), is.getInsumo(), "LIBERACION_RESERVA", cant, actual, actual, costoUnitario, r.getUsuario(), "Liberación total pedido #" + pedidoId, pedidoId));
        }
    }

    @Override @Transactional
    public boolean convertirReservaAConsumo(Long pedidoId) {
        List<KardexMovimiento> reservas = kardexRepository.findByPedidoIdAndTipoMovimiento(pedidoId, TIPO_MOVIMIENTO_RESERVA);
        boolean requiereRevision = false;
        for (KardexMovimiento r : reservas) {
            InsumoSede is = obtenerInventarioFisico(r.getSedeId(), r.getInsumo().getId());
            BigDecimal cant = r.getCantidad();
            
            BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;

            if (ant.compareTo(cant) < 0) {
                requiereRevision = true;
                is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
                insumoSedeRepository.save(is);
                continue;
            }
            BigDecimal post = ant.subtract(cant);
            is.setStockActual(post);
            is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            BigDecimal costoUnitario = is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO;
            registrarMovimientoKardex(new DatosKardex(is.getSedeId(), is.getInsumo(), "CONSUMO_PRODUCCION", cant, ant, post, costoUnitario, r.getUsuario(), "Consumo producción pedido #" + pedidoId, pedidoId));
        }
        return requiereRevision;
    }

    // 🔥 FIX 500: PASAMOS SEDE_ID DIRECTAMENTE DESDE EL PEDIDO
    @Override @Transactional
    public boolean convertirItemsAConsumo(Long pedidoId, List<PedidoDetalle> detalles) {
        if (detalles.isEmpty()) return false;
        Long sedeIdDelPedido = detalles.get(0).getPedido().getSedeId();

        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo, sedeIdDelPedido);
        Map<Long, BigDecimal> costoUnitarioPorDetalle = calcularCostoUnitarioPorDetalle(detalles, insumosPorId);
        
        Usuario operador = detalles.get(0).getPedido().getMozo();
        boolean reqRevision = consumirInsumos(pedidoId, totalPorInsumo, insumosPorId, operador);
        persistirCostoUnitarioConsumido(detalles, costoUnitarioPorDetalle);
        return reqRevision;
    }

    // 🔥 FIX 500: PASAMOS SEDE_ID DIRECTAMENTE DESDE EL PEDIDO
    @Override @Transactional
    public void liberarReservaDeItems(Long pedidoId, List<PedidoDetalle> detalles) {
        if (detalles.isEmpty()) return;
        Long sedeIdDelPedido = detalles.get(0).getPedido().getSedeId();

        Map<Long, BigDecimal> totalPorInsumo = calcularTotalPorInsumo(detalles);
        Map<Long, InsumoSede> insumosPorId = cargarInventarioSede(totalPorInsumo, sedeIdDelPedido);
        Usuario operador = detalles.get(0).getPedido().getMozo();

        for (Map.Entry<Long, BigDecimal> entry : totalPorInsumo.entrySet()) {
            InsumoSede is = insumosPorId.get(entry.getKey());
            BigDecimal cant = entry.getValue();
            
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;
            is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
            insumoSedeRepository.save(is);
            
            BigDecimal actual = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal costoUnitario = is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO;
            registrarMovimientoKardex(new DatosKardex(is.getSedeId(), is.getInsumo(), "LIBERACION_RESERVA", cant, actual, actual, costoUnitario, operador, "Liberación parcial ítem en pedido #" + pedidoId, pedidoId));
        }
    }

    private Map<Long, BigDecimal> calcularTotalPorInsumo(List<PedidoDetalle> detalles) {
        Map<Long, BigDecimal> total = new LinkedHashMap<>();
        for (PedidoDetalle d : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(d.getProducto().getId());
            BigDecimal factor = new BigDecimal(d.getCantidad());
            for (RecetaDetalle rd : receta) total.merge(rd.getInsumo().getId(), rd.getCantidadRequerida().multiply(factor), BigDecimal::add);
        }
        return total;
    }

    // 🔥 FIX 500: SE RECIBE EL SEDE_ID COMO PARÁMETRO SEGURO, YA NO DEPENDE DEL HILO
    private Map<Long, InsumoSede> cargarInventarioSede(Map<Long, BigDecimal> totalPorInsumo, Long sedeIdDelPedido) {
        Map<Long, InsumoSede> map = new LinkedHashMap<>();
        for (Long insumoId : totalPorInsumo.keySet()) {
            map.put(insumoId, obtenerInventarioFisico(sedeIdDelPedido, insumoId));
        }
        return map;
    }

    private List<InsumoFaltanteDTO> validarDisponibilidadReserva(Map<Long, BigDecimal> req, Map<Long, InsumoSede> inv) {
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : req.entrySet()) {
            InsumoSede is = inv.get(entry.getKey());
            if (is == null) continue;
            BigDecimal necesario = entry.getValue();
            
            BigDecimal stockActual = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;
            
            BigDecimal disp = stockActual.subtract(stockRes);
            if (disp.compareTo(necesario) < 0) faltantes.add(new InsumoFaltanteDTO(is.getInsumo().getNombre(), disp.max(BigDecimal.ZERO), necesario));
        }
        return faltantes;
    }

    private Map<Long, BigDecimal> calcularCostoUnitarioPorDetalle(List<PedidoDetalle> detalles, Map<Long, InsumoSede> inv) {
        Map<Long, BigDecimal> costoDetalle = new LinkedHashMap<>();
        for (PedidoDetalle d : detalles) {
            List<RecetaDetalle> receta = recetaDetalleRepository.findByProductoId(d.getProducto().getId());
            if (receta.isEmpty()) {
                BigDecimal cr = d.getProducto().getCostoReferencial();
                costoDetalle.put(d.getId(), cr != null ? cr : BigDecimal.ZERO);
                continue;
            }
            BigDecimal costoItem = BigDecimal.ZERO;
            for (RecetaDetalle rd : receta) {
                InsumoSede is = inv.get(rd.getInsumo().getId());
                if (is != null && is.getCostoUnitario() != null) costoItem = costoItem.add(rd.getCantidadRequerida().multiply(is.getCostoUnitario()));
            }
            costoDetalle.put(d.getId(), costoItem);
        }
        return costoDetalle;
    }

    private boolean consumirInsumos(Long pedidoId, Map<Long, BigDecimal> req, Map<Long, InsumoSede> inv, Usuario operador) {
        boolean reqRev = false;
        for (Map.Entry<Long, BigDecimal> entry : req.entrySet()) {
            InsumoSede is = inv.get(entry.getKey());
            if (is == null) continue;
            BigDecimal cant = entry.getValue();
            
            BigDecimal ant = is.getStockActual() != null ? is.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockRes = is.getStockReservado() != null ? is.getStockReservado() : BigDecimal.ZERO;
            
            if (ant.compareTo(cant) < 0) {
                reqRev = true;
                is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
            } else {
                BigDecimal post = ant.subtract(cant);
                is.setStockActual(post);
                is.setStockReservado(stockRes.subtract(cant).max(BigDecimal.ZERO));
                BigDecimal costoUnit = is.getCostoUnitario() != null ? is.getCostoUnitario() : BigDecimal.ZERO;
                registrarMovimientoKardex(new DatosKardex(is.getSedeId(), is.getInsumo(), "CONSUMO_PRODUCCION", cant, ant, post, costoUnit, operador, "Consumo pedido #" + pedidoId, pedidoId));
            }
            insumoSedeRepository.save(is);
        }
        return reqRev;
    }

    private void persistirCostoUnitarioConsumido(List<PedidoDetalle> detalles, Map<Long, BigDecimal> costoDetalle) {
        for (PedidoDetalle d : detalles) {
            BigDecimal c = costoDetalle.get(d.getId());
            if (c != null) { d.setCostoUnitarioConsumido(c); detalleRepository.save(d); }
        }
    }
}