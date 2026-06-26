package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.repositories.*;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class InventarioServiceImpl implements IInventarioService {

    private static final String INSUMO_NO_ENCONTRADO = "Insumo no encontrado";

    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final KardexMovimientoRepository kardexRepository;

    public InventarioServiceImpl(CategoriaRepository categoriaRepository,
                                 InsumoRepository insumoRepository,
                                 ProductoRepository productoRepository,
                                 RecetaDetalleRepository recetaDetalleRepository,
                                 KardexMovimientoRepository kardexRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
        this.productoRepository = productoRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.kardexRepository = kardexRepository;
    }

    @Override
    @Transactional
    public Categoria crearCategoria(Categoria categoria) { return categoriaRepository.save(categoria); }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() { return categoriaRepository.findAll(); }

    @Override
    @Transactional
    public Insumo crearInsumo(Insumo insumo) { return insumoRepository.save(insumo); }

    @Override
    @Transactional(readOnly = true)
    public List<Insumo> listarInsumos() { return insumoRepository.findAll(); }

    @Override
    @Transactional(readOnly = true)
    public List<Insumo> listarInsumosConStockBajo() { return insumoRepository.findInsumosConStockBajo(); }

    @Override
    @Transactional
    public Producto crearProducto(Producto producto) { return productoRepository.save(producto); }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() { return productoRepository.findAll(); }

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

        // Cálculo de Promedio Ponderado para el nuevo Costo Unitario
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
        BigDecimal stockPosterior = dto.getEsPositivo()
                ? stockAnterior.add(dto.getCantidad())
                : stockAnterior.subtract(dto.getCantidad());

        if (stockPosterior.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el stock en negativo");
        }

        insumo.setStockActual(stockPosterior);
        insumoRepository.save(insumo);

        String tipo = dto.getEsPositivo() ? "ENTRADA_AJUSTE" : "SALIDA_AJUSTE";
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
}
