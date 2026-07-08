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

    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody CategoriaRequestDTO dto) {
        Categoria c = new Categoria();
        c.setNombre(dto.getNombre());
        return new ResponseEntity<>(inventarioService.crearCategoria(c), HttpStatus.CREATED);
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarCategoria(id, dto));
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarCategoria(@PathVariable Long id) {
        inventarioService.desactivarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/categorias/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> activarCategoria(@PathVariable Long id) {
        inventarioService.activarCategoria(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Producto> crearProducto(@RequestBody ProductoRequestDTO dto) {
        Producto p = new Producto();
        p.setNombre(dto.getNombre());
        p.setPrecioVenta(dto.getPrecioVenta());
        p.setTagsBusqueda(dto.getTagsBusqueda());
        p.setEsPreparado(dto.getEsPreparado());
        p.setTiempoPreparacionMinutos(dto.getTiempoPreparacionMinutos());
        return new ResponseEntity<>(inventarioService.crearProducto(p), HttpStatus.CREATED);
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        inventarioService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/productos/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> activarProducto(@PathVariable Long id) {
        inventarioService.activarProducto(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    @PostMapping("/productos/{id}/receta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<RecetaDetalle> agregarInsumoAReceta(@PathVariable Long id, @RequestBody RecetaRequestDTO dto) {
        return new ResponseEntity<>(inventarioService.agregarInsumoAReceta(id, dto.getInsumoId(), dto.getCantidad(), dto.getUnidadMedida()), HttpStatus.CREATED);
    }

    @GetMapping("/productos/{id}/receta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<RecetaDetalle>> obtenerReceta(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(id));
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody InsumoRequestDTO dto) {
        Insumo i = new Insumo();
        i.setNombre(dto.getNombre());
        i.setUnidadMedida(dto.getUnidadMedida());
        return new ResponseEntity<>(inventarioService.crearInsumo(i), HttpStatus.CREATED);
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Insumo> actualizarInsumo(@PathVariable Long id, @RequestBody InsumoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarInsumo(id, dto));
    }

    @DeleteMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarInsumo(@PathVariable Long id) {
        inventarioService.desactivarInsumo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/insumos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Insumo>> listarInsumos() {
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

    // FASE 6: Se agrega sedeId como filtro opcional
    @GetMapping("/insumos/stock-bajo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<InsumoBajoStockDTO>> listarInsumosConStockBajo(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(inventarioService.listarInsumosConStockBajo(sedeId));
    }

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

    // FASE 6: Se agrega sedeId como filtro opcional
    @GetMapping("/insumos/{id}/kardex")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<Object> listarKardex(
            @PathVariable Long id, 
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(inventarioService.listarKardexPorInsumo(id, sedeId));
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("Usuario autenticado inválido");
        }

        Long usuarioId = userDetails.getUsuarioId();
        if (usuarioId == null) {
            throw new IllegalStateException("ID de usuario no encontrado en el token");
        }

        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no existe en la base de datos"));
    }
}