package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.AjusteInventarioRequestDTO;
import com.rutadelsabor.core.dto.request.EntradaAlmacenRequestDTO;
import com.rutadelsabor.core.dto.request.InsumoRequestDTO;
import com.rutadelsabor.core.dto.request.MermaRequestDTO;
import com.rutadelsabor.core.dto.request.ProductoRequestDTO;
import com.rutadelsabor.core.dto.request.RecetaRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.*;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final IInventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(IInventarioService inventarioService, UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ─── CATEGORÍAS ──────────────────────────────────────────────────────────

    @GetMapping("/categorias")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearCategoria(categoria));
    }

    // ─── PRODUCTOS ────────────────────────────────────────────────────────────

    @GetMapping("/productos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_CAJERO') or hasAuthority('ROLE_MOZO')")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<Producto> crearProducto(@RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearProducto(producto));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> desactivarProducto(@PathVariable Long id) {
        inventarioService.desactivarProducto(id);
        return ResponseEntity.ok("Producto desactivado exitosamente.");
    }

    // ─── INSUMOS ──────────────────────────────────────────────────────────────

    @GetMapping("/insumos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<List<Insumo>> listarInsumos() {
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody Insumo insumo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearInsumo(insumo));
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<Insumo> actualizarInsumo(@PathVariable Long id, @RequestBody InsumoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarInsumo(id, dto));
    }

    @DeleteMapping("/insumos/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> desactivarInsumo(@PathVariable Long id) {
        inventarioService.desactivarInsumo(id);
        return ResponseEntity.ok("Insumo desactivado exitosamente.");
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<List<Insumo>> alertasStockBajo() {
        return ResponseEntity.ok(inventarioService.listarInsumosConStockBajo());
    }

    // ─── RECETAS ──────────────────────────────────────────────────────────────

    @GetMapping("/recetas/{productoId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE') or hasAuthority('ROLE_COCINA')")
    public ResponseEntity<List<RecetaDetalle>> obtenerReceta(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(productoId));
    }

    @PostMapping("/recetas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<RecetaDetalle> agregarInsumoAReceta(@RequestBody RecetaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                inventarioService.agregarInsumoAReceta(dto.getProductoId(), dto.getInsumoId(), dto.getCantidadRequerida(), dto.getUnidadMedida())
        );
    }

    // ─── KARDEX ───────────────────────────────────────────────────────────────

    @GetMapping("/kardex/{insumoId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<List<KardexMovimiento>> listarKardex(@PathVariable Long insumoId) {
        return ResponseEntity.ok(inventarioService.listarKardexPorInsumo(insumoId));
    }

    // ─── MOVIMIENTOS (ENTRADA / MERMA / AJUSTE) ───────────────────────────────

    @PostMapping("/entradas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarEntrada(@RequestBody EntradaAlmacenRequestDTO dto, Authentication auth) {
        inventarioService.registrarEntrada(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Entrada registrada y Kardex actualizado (Promedio Ponderado recalculado).");
    }

    @PostMapping("/mermas")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarMerma(@RequestBody MermaRequestDTO dto, Authentication auth) {
        inventarioService.registrarMerma(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Merma registrada y stock descontado exitosamente.");
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_GERENTE')")
    public ResponseEntity<String> registrarAjuste(@RequestBody AjusteInventarioRequestDTO dto, Authentication auth) {
        inventarioService.registrarAjuste(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Ajuste de inventario aplicado exitosamente.");
    }

    private Usuario obtenerUsuarioAuth(Authentication auth) {
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado en el sistema"));
    }
}
