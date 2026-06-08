package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Receta;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    @Autowired
    private IInventarioService inventarioService;

    // --- CATEGORÍAS ---
    @PostMapping("/categorias")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        return new ResponseEntity<>(inventarioService.crearCategoria(categoria), HttpStatus.CREATED);
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    // --- INSUMOS ---
    @PostMapping("/insumos")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody Insumo insumo) {
        return new ResponseEntity<>(inventarioService.crearInsumo(insumo), HttpStatus.CREATED);
    }

    @GetMapping("/insumos")
    public ResponseEntity<List<Insumo>> listarInsumos() {
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

    // --- PRODUCTOS ---
    @PostMapping("/productos")
    public ResponseEntity<Producto> crearProducto(@RequestBody Producto producto) {
        return new ResponseEntity<>(inventarioService.crearProducto(producto), HttpStatus.CREATED);
    }

    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    // --- RECETAS ---
    @PostMapping("/recetas")
    public ResponseEntity<Receta> agregarInsumoAReceta(
            @RequestParam Long productoId,
            @RequestParam Long insumoId,
            @RequestParam BigDecimal cantidad) {
        Receta receta = inventarioService.agregarInsumoAReceta(productoId, insumoId, cantidad);
        return new ResponseEntity<>(receta, HttpStatus.CREATED);
    }

    @GetMapping("/recetas/{productoId}")
    public ResponseEntity<List<Receta>> verReceta(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(productoId));
    }
}