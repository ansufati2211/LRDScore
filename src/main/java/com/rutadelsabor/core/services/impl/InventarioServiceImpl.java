package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Receta;
import com.rutadelsabor.core.repositories.CategoriaRepository;
import com.rutadelsabor.core.repositories.InsumoRepository;
import com.rutadelsabor.core.repositories.ProductoRepository;
import com.rutadelsabor.core.repositories.RecetaRepository;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventarioServiceImpl implements IInventarioService {

    // 1. Declaramos las dependencias como finales e inmutables (Resuelve java:S6813)
    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;

    // 2. Inyección por Constructor 
    public InventarioServiceImpl(CategoriaRepository categoriaRepository, 
                                 InsumoRepository insumoRepository, 
                                 ProductoRepository productoRepository, 
                                 RecetaRepository recetaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
    }

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
        // Usamos la excepción especializada de la Fase 1 (Resuelve java:S112)
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con el ID: " + productoId));
        
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con el ID: " + insumoId));

        Receta receta = new Receta();
        receta.setProducto(producto);
        receta.setInsumo(insumo);
        receta.setCantidadRequerida(cantidad);

        return recetaRepository.save(receta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receta> obtenerRecetaPorProducto(Long productoId) {
        // OPTIMIZACIÓN DE COHESIÓN: Consultamos directamente por el ID a través del repositorio,
        // evitando cargar toda la entidad Producto si solo necesitamos su ID
        return recetaRepository.findByProductoId(productoId);
    }
}