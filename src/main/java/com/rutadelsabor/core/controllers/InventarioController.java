package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.RecetaDetalle;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {

    private final IInventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(IInventarioService inventarioService, UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // --- CATEGORÍAS ---
    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody CategoriaRequestDTO dto) {
        Categoria cat = new Categoria();
        cat.setNombre(dto.getNombre());
        return new ResponseEntity<>(inventarioService.crearCategoria(cat), HttpStatus.CREATED);
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarCategoria(id, dto));
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarCategoria(@PathVariable Long id) {
        inventarioService.desactivarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    // --- INSUMOS (CATÁLOGO MULTI-SEDE) ---
    @PostMapping("/insumos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody InsumoRequestDTO dto) {
        Insumo insumo = new Insumo();
        insumo.setNombre(dto.getNombre());
        insumo.setUnidadMedida(dto.getUnidadMedida());
        // FIX: Ya no se setea stock aquí porque el Insumo es solo un catálogo
        return new ResponseEntity<>(inventarioService.crearInsumo(insumo), HttpStatus.CREATED);
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Insumo> actualizarInsumo(@PathVariable Long id, @RequestBody InsumoRequestDTO dto) {
        // FIX: El servicio actualizado (abajo) ignorará el stockMinimo en Insumo y solo actualizará Nombre/Unidad
        return ResponseEntity.ok(inventarioService.actualizarInsumo(id, dto));
    }

    @DeleteMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarInsumo(@PathVariable Long id) {
        inventarioService.desactivarInsumo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/insumos")
    public ResponseEntity<List<Insumo>> listarInsumos() {
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

@GetMapping("/insumos/stock-bajo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<InsumoBajoStockDTO>> listarInsumosConStockBajo() {
        return ResponseEntity.ok(inventarioService.listarInsumosConStockBajo());
    }

    // --- PRODUCTOS ---
    @PostMapping("/productos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Producto> crearProducto(@RequestBody ProductoRequestDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setTagsBusqueda(dto.getTagsBusqueda());
        producto.setEsPreparado(dto.getEsPreparado());
        producto.setTiempoPreparacionMinutos(dto.getTiempoPreparacionMinutos());
        return new ResponseEntity<>(inventarioService.crearProducto(producto), HttpStatus.CREATED);
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        inventarioService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    // --- RECETAS ---
@PostMapping("/productos/{id}/receta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<RecetaDetalle> agregarInsumoAReceta(@PathVariable Long id, @RequestBody RecetaRequestDTO dto) {
        // Se cambió getCantidad() por getCantidadRequerida()
        return new ResponseEntity<>(inventarioService.agregarInsumoAReceta(
                id, dto.getInsumoId(), dto.getCantidadRequerida(), dto.getUnidadMedida()), HttpStatus.CREATED);
    }
    @GetMapping("/productos/{id}/receta")
    public ResponseEntity<List<RecetaDetalle>> obtenerReceta(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(id));
    }

    // --- MOVIMIENTOS KARDEX (AHORA POR SEDE) ---
    @PostMapping("/kardex/entrada")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Void> registrarEntrada(@RequestBody EntradaAlmacenRequestDTO dto, Authentication auth) {
        inventarioService.registrarEntrada(dto, obtenerUsuario(auth));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/kardex/merma")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE', 'COCINA')")
    public ResponseEntity<Void> registrarMerma(@RequestBody MermaRequestDTO dto, Authentication auth) {
        inventarioService.registrarMerma(dto, obtenerUsuario(auth));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/kardex/ajuste")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Void> registrarAjuste(@RequestBody AjusteInventarioRequestDTO dto, Authentication auth) {
        inventarioService.registrarAjuste(dto, obtenerUsuario(auth));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/insumos/{id}/kardex")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Object> listarKardex(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.listarKardexPorInsumo(id));
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("Usuario autenticado inválido");
        }

        Long usuarioId = userDetails.getUsuarioId();
        if (usuarioId == null) {
            throw new IllegalStateException("ID de usuario inválido");
        }

        return usuarioRepository.findById(usuarioId).orElseThrow();
    }
}