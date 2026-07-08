package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.CategoriaRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.InsumoRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.dto.request.ProductoRequestDTO;
import com.rutadelsabor.core.models.entities.*;

import java.math.BigDecimal;
import java.util.List;

public interface IInventarioService {
    // --- CATEGORIAS ---
    Categoria crearCategoria(Categoria categoria);
    Categoria actualizarCategoria(Long id, CategoriaRequestDTO dto);
    void desactivarCategoria(Long id);
    List<Categoria> listarCategorias();

    // --- INSUMOS ---
    Insumo crearInsumo(Insumo insumo);
    Insumo actualizarInsumo(Long id, InsumoRequestDTO dto);
    void desactivarInsumo(Long id);
    List<Insumo> listarInsumos();
    List<Insumo> listarInsumosConStockBajo();

    // --- PRODUCTOS ---
    Producto crearProducto(Producto producto);
    Producto actualizarProducto(Long id, ProductoRequestDTO dto);
    void desactivarProducto(Long id);
    List<Producto> listarProductos();

    void activarCategoria(Long id);
    void activarProducto(Long id);

    // --- RECETAS ---
    RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida);
    List<RecetaDetalle> obtenerRecetaPorProducto(Long productoId);

    // --- KARDEX ---
    List<KardexMovimiento> listarKardexPorInsumo(Long insumoId);
    void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usuario);
    void registrarMerma(MermaRequestDTO dto, Usuario usuario);
    void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usuario);

    // Módulo 3: reserva/consumo/liberación por ciclo de pedido
    void reservarInsumosParaPedido(Long pedidoId, List<PedidoDetalle> detalles);
    void liberarReservaDePedido(Long pedidoId);
    boolean convertirReservaAConsumo(Long pedidoId);

    // Módulo 4: versiones recipe-based para soporte multi-comanda sin doble procesamiento
    boolean convertirItemsAConsumo(Long pedidoId, List<PedidoDetalle> detalles);
    void liberarReservaDeItems(Long pedidoId, List<PedidoDetalle> detalles);
}