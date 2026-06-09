package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.CategoriaRequestDTO;
import com.rutadelsabor.core.dto.request.InsumoRequestDTO;
import com.rutadelsabor.core.dto.request.ProductoRequestDTO;
import com.rutadelsabor.core.dto.request.RecetaRequestDTO;
import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Receta;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    // 1. Variable final y sin @Autowired (Resuelve java:S6813)
    private final IInventarioService inventarioService;

    // 2. Inyección por Constructor
    public InventarioController(IInventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    // --- CATEGORÍAS ---
    @PostMapping("/categorias")
    // 3. Usamos DTO para proteger la Entidad (Resuelve java:S4684)
    public ResponseEntity<Categoria> crearCategoria(@RequestBody CategoriaRequestDTO request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        
        return new ResponseEntity<>(inventarioService.crearCategoria(categoria), HttpStatus.CREATED);
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    // --- INSUMOS ---
    @PostMapping("/insumos")
    // 3. Usamos DTO para proteger la Entidad (Resuelve java:S4684)
    public ResponseEntity<Insumo> crearInsumo(@RequestBody InsumoRequestDTO request) {
        Insumo insumo = new Insumo();
        insumo.setNombre(request.getNombre());
        insumo.setUnidadMedida(request.getUnidadMedida());
        insumo.setStockActual(request.getStockActual());
        
        return new ResponseEntity<>(inventarioService.crearInsumo(insumo), HttpStatus.CREATED);
    }

    @GetMapping("/insumos")
    public ResponseEntity<List<Insumo>> listarInsumos() {
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

    // --- PRODUCTOS ---
    @PostMapping("/productos")
    // 3. Usamos DTO para proteger la Entidad (Resuelve java:S4684)
    public ResponseEntity<Producto> crearProducto(@RequestBody ProductoRequestDTO request) {
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setTagsBusqueda(request.getTagsBusqueda());
        
        // Mapeamos la relación con la Categoría solo usando su ID
        Categoria categoriaRef = new Categoria();
        categoriaRef.setId(request.getCategoriaId());
        producto.setCategoria(categoriaRef);
        
        return new ResponseEntity<>(inventarioService.crearProducto(producto), HttpStatus.CREATED);
    }

    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    // --- RECETAS ---
    @PostMapping("/recetas")
    // Unificamos usando el RecetaRequestDTO que ya existía en tu proyecto
    public ResponseEntity<Receta> agregarInsumoAReceta(@RequestBody RecetaRequestDTO request) {
        Receta receta = inventarioService.agregarInsumoAReceta(
                request.getProductoId(), 
                request.getInsumoId(), 
                request.getCantidadRequerida()
        );
        return new ResponseEntity<>(receta, HttpStatus.CREATED);
    }

    @GetMapping("/recetas/{productoId}")
    public ResponseEntity<List<Receta>> verReceta(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(productoId));
    }
}