package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Receta;
import com.rutadelsabor.core.repositories.CategoriaRepository;
import com.rutadelsabor.core.repositories.InsumoRepository;
import com.rutadelsabor.core.repositories.ProductoRepository;
import com.rutadelsabor.core.repositories.RecetaRepository;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventarioServiceImpl implements IInventarioService {

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private InsumoRepository insumoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private RecetaRepository recetaRepository;

    @Override
    @Transactional
    public Categoria crearCategoria(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @Override
    @Transactional
    public Insumo crearInsumo(Insumo insumo) {
        return insumoRepository.save(insumo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Insumo> listarInsumos() {
        return insumoRepository.findAll();
    }

    @Override
    @Transactional
    public Producto crearProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional
    public Receta agregarInsumoAReceta(Long productoId, Long insumoId, BigDecimal cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));

        Receta receta = new Receta();
        receta.setProducto(producto);
        receta.setInsumo(insumo);
        receta.setCantidadRequerida(cantidad);

        return recetaRepository.save(receta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receta> obtenerRecetaPorProducto(Long productoId) {
        // En un escenario ideal tendrías un método findByProductoId en el Repositorio, 
        // pero por ahora filtramos usando streams de Java para simplificar.
        return recetaRepository.findAll().stream()
                .filter(r -> r.getProducto().getId().equals(productoId))
                .toList();
    }
}