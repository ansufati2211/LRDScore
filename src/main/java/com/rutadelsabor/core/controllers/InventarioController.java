package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;
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
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

@SuppressWarnings("squid:S4684")
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {
    
    private final IInventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(IInventarioService inventarioService, UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ==========================================
    // CATEGORÍAS
    // ==========================================
    @GetMapping("/categorias")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO')")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(inventarioService.listarCategorias());
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearCategoria(categoria));
    }

    // Faltaba este endpoint para actualizar
    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarCategoria(id, dto));
    }

    // Faltaba este endpoint para inhabilitar
    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> desactivarCategoria(@PathVariable Long id) {
        inventarioService.desactivarCategoria(id);
        return ResponseEntity.ok("Categoría desactivada exitosamente.");
    }

    // 🔥 FIX: Endpoint faltante para Activar Categoría
    @PutMapping("/categorias/{id}/activar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> activarCategoria(@PathVariable Long id) {
        inventarioService.activarCategoria(id);
        return ResponseEntity.ok("Categoría activada exitosamente.");
    }

    // ==========================================
    // PRODUCTOS
    // ==========================================
    @GetMapping("/productos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO')")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Producto> crearProducto(@RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearProducto(producto));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> desactivarProducto(@PathVariable Long id) {
        inventarioService.desactivarProducto(id);
        return ResponseEntity.ok("Producto desactivado exitosamente.");
    }

    // 🔥 FIX: Endpoint faltante para Activar Producto
    @PutMapping("/productos/{id}/activar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> activarProducto(@PathVariable Long id) {
        inventarioService.activarProducto(id);
        return ResponseEntity.ok("Producto activado exitosamente.");
    }

    // ==========================================
    // INSUMOS
    // ==========================================
    @GetMapping("/insumos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<?> listarInsumos(@RequestParam(required = false) Long paramSedeId) {
        
        // 🔥 FIX: Si React no envía la sede, la extraemos automáticamente de la sesión segura del usuario
        Long sedeEfectiva = (paramSedeId != null) ? paramSedeId : TenantContext.getCurrentSede();

        if (sedeEfectiva != null) {
            // Esto devuelve el catálogo "fusionado" con la tabla insumo_sede (Trae stockActual y costoUnitario)
            return ResponseEntity.ok(inventarioService.listarInsumosConCosto(sedeEfectiva));
        }
        
        // Fallback: Catálogo vacío si por algún motivo extremo no hay sede
        return ResponseEntity.ok(inventarioService.listarInsumos());
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody Insumo insumo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearInsumo(insumo));
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<Insumo> actualizarInsumo(@PathVariable Long id, @RequestBody InsumoRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizarInsumo(id, dto));
    }

    @DeleteMapping("/insumos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> desactivarInsumo(@PathVariable Long id) {
        inventarioService.desactivarInsumo(id);
        return ResponseEntity.ok("Insumo desactivado exitosamente.");
    }

    // 🔥 FIX: Endpoint faltante para Activar Insumo
    @PutMapping("/insumos/{id}/activar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> activarInsumo(@PathVariable Long id) {
        inventarioService.activarInsumo(id);
        return ResponseEntity.ok("Insumo activado exitosamente.");
    }

    // ==========================================
    // ALERTAS, RECETAS Y KARDEX
    // ==========================================
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<List<InsumoBajoStockDTO>> alertasStockBajo() {
        return ResponseEntity.ok(inventarioService.listarInsumosConStockBajo(TenantContext.getCurrentSede()));
    }

    @GetMapping("/recetas/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_COCINA')")
    public ResponseEntity<List<RecetaDetalle>> obtenerReceta(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.obtenerRecetaPorProducto(productoId));
    }

    @PostMapping("/recetas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<RecetaDetalle> agregarInsumoAReceta(@RequestBody RecetaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                inventarioService.agregarInsumoAReceta(dto.getProductoId(), dto.getInsumoId(), dto.getCantidadRequerida(), dto.getUnidadMedida())
        );
    }

    @PostMapping("/productos/{productoId}/receta")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> actualizarRecetaCompleta(
            @PathVariable Long productoId, 
            @RequestBody List<Map<String, Object>> detalles) {
        
        Map<Long, BigDecimal> insumosYCantidades = new HashMap<>();
        for (Map<String, Object> d : detalles) {
            Long insumoId = Long.valueOf(d.get("insumoId").toString());
            BigDecimal cant = new BigDecimal(d.get("cantidadUsada").toString());
            insumosYCantidades.put(insumoId, cant);
        }
        
        inventarioService.actualizarRecetaCompleta(productoId, insumosYCantidades);
        return ResponseEntity.ok("Receta actualizada con éxito");
    }

    @GetMapping("/kardex/{insumoId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<List<KardexMovimiento>> listarKardex(@PathVariable Long insumoId) {
        return ResponseEntity.ok(inventarioService.listarKardexPorInsumo(insumoId, TenantContext.getCurrentSede()));
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> registrarEntrada(@RequestBody EntradaAlmacenRequestDTO dto, Authentication auth) {
        inventarioService.registrarEntrada(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Entrada registrada y Kardex actualizado.");
    }

    @PostMapping("/mermas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> registrarMerma(@RequestBody MermaRequestDTO dto, Authentication auth) {
        inventarioService.registrarMerma(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Merma registrada y stock descontado exitosamente.");
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE')")
    public ResponseEntity<String> registrarAjuste(@RequestBody AjusteInventarioRequestDTO dto, Authentication auth) {
        inventarioService.registrarAjuste(dto, obtenerUsuarioAuth(auth));
        return ResponseEntity.ok("Ajuste de inventario aplicado exitosamente.");
    }

    private Usuario obtenerUsuarioAuth(Authentication auth) {
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}