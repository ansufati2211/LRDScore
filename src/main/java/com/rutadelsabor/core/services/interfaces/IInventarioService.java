package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.models.entities.*;

import java.math.BigDecimal;
import java.util.List;

public interface IInventarioService {
    Categoria crearCategoria(Categoria categoria);
    List<Categoria> listarCategorias();
    
    Insumo crearInsumo(Insumo insumo);
    List<Insumo> listarInsumos();
    
    Producto crearProducto(Producto producto);
    List<Producto> listarProductos();
    
    RecetaDetalle agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad, String unidadMedida);
    List<RecetaDetalle> obtenerRecetaPorProducto(Long productoId);

    // NUEVOS MÉTODOS DEL KARDEX
    void registrarEntrada(EntradaAlmacenRequestDTO dto, Usuario usuario);
    void registrarMerma(MermaRequestDTO dto, Usuario usuario);
    void registrarAjuste(AjusteInventarioRequestDTO dto, Usuario usuario);
}