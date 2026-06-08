package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Receta;

import java.math.BigDecimal;
import java.util.List;

public interface IInventarioService {
    // Categorías
    Categoria crearCategoria(Categoria categoria);
    List<Categoria> listarCategorias();

    // Insumos (Almacén)
    Insumo crearInsumo(Insumo insumo);
    List<Insumo> listarInsumos();

    // Productos (Carta)
    Producto crearProducto(Producto producto);
    List<Producto> listarProductos();

    // Recetas (Escandallo)
    Receta agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad);
    List<Receta> obtenerRecetaPorProducto(Long productoId);
}