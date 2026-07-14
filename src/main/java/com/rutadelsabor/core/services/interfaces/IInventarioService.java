package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;
import com.rutadelsabor.core.models.entities.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IInventarioService {
    Categoria crearCategoria(Categoria c);
    Categoria actualizarCategoria(Long id, CategoriaRequestDTO dto);
    void desactivarCategoria(Long id);
    void activarCategoria(Long id);
    List<Categoria> listarCategorias();

    Producto crearProducto(Producto p);
    Producto actualizarProducto(Long id, ProductoRequestDTO dto);
    void desactivarProducto(Long id);
    void activarProducto(Long id);
    List<Producto> listarProductos();

    RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida);
    List<RecetaDetalle> obtenerRecetaPorProducto(Long productoId);

    // 👇 AQUÍ ESTÁN LOS MÉTODOS COMPLETOS PARA REACT 👇
    void actualizarRecetaCompleta(Long productoId, Map<Long, BigDecimal> insumosYCantidades);
    List<Map<String, Object>> obtenerRecetaFormateada(Long productoId); // <-- ESTA ERA LA LÍNEA QUE FALTABA
    List<Map<String, Object>> listarInsumosConCosto(Long sedeId);

    Insumo crearInsumo(Insumo i);
    Insumo actualizarInsumo(Long id, InsumoRequestDTO dto);
    void desactivarInsumo(Long id);
    void activarInsumo(Long id); 
    List<Insumo> listarInsumos();

    List<InsumoBajoStockDTO> listarInsumosConStockBajo(Long sedeIdFiltro);
    List<KardexMovimiento> listarKardexPorInsumo(Long insumoId, Long sedeIdFiltro);

    void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usr);
    void registrarMerma(MermaRequestDTO dto, Usuario usr);
    void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usr);

    void reservarInsumosParaPedido(Long pedidoId, List<PedidoDetalle> detalles);
    void liberarReservaDePedido(Long pedidoId);
    boolean convertirReservaAConsumo(Long pedidoId);
    boolean convertirItemsAConsumo(Long pedidoId, List<PedidoDetalle> detalles);
    void liberarReservaDeItems(Long pedidoId, List<PedidoDetalle> detalles);
}