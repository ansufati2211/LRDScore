package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.InsumoRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.dto.request.ProductoRequestDTO;
import com.rutadelsabor.core.models.entities.*;

import java.math.BigDecimal;
import java.util.List;

public interface IInventarioService {
    Categoria crearCategoria(Categoria categoria);
    List<Categoria> listarCategorias();

    Insumo crearInsumo(Insumo insumo);
    Insumo actualizarInsumo(Long id, InsumoRequestDTO dto);
    void desactivarInsumo(Long id);
    List<Insumo> listarInsumos();
    List<Insumo> listarInsumosConStockBajo();

    Producto crearProducto(Producto producto);
    Producto actualizarProducto(Long id, ProductoRequestDTO dto);
    void desactivarProducto(Long id);
    List<Producto> listarProductos();

    RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida);
    List<RecetaDetalle> obtenerRecetaPorProducto(Long productoId);

    List<KardexMovimiento> listarKardexPorInsumo(Long insumoId);

    void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usuario);
    void registrarMerma(MermaRequestDTO dto, Usuario usuario);
    void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usuario);
}
