package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.*;
import com.rutadelsabor.core.dto.response.InsumoBajoStockDTO;
import com.rutadelsabor.core.models.entities.Categoria;
import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.Producto;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IInventarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        c.setEmpresaId(TenantContext.getCurrentTenant());
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
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_MOZO', 'ROLE_CAJERO', 'ROLE_COCINA')")
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
        p.setEmpresaId(TenantContext.getCurrentTenant());
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
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE', 'MOZO', 'CAJERO')")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    // =====================================================================
    // ENDPOINTS DE RECETA
    // =====================================================================

    public static class RecetaPayloadDTO {
        public Long insumoId;
        public java.math.BigDecimal cantidadUsada;
    }

    @PostMapping("/productos/{id}/receta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<?> guardarRecetaCompleta(@PathVariable Long id, @RequestBody List<RecetaPayloadDTO> payload) {
        try {
            Map<Long, java.math.BigDecimal> mapa = new HashMap<>();
            if (payload != null) {
                for (RecetaPayloadDTO d : payload) {
                    mapa.put(d.insumoId, d.cantidadUsada);
                }
            }
            inventarioService.actualizarRecetaCompleta(id, mapa);
            return ResponseEntity.ok().body(Map.of("message", "Receta guardada exitosamente"));
        } catch (Exception e) {
            e.printStackTrace(); 
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error Nulo";
            String causa = e.getCause() != null ? e.getCause().getMessage() : "No hay detalles adicionales";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", mensaje, "causa", causa));
        }
    }

    @GetMapping("/productos/{id}/receta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<?> obtenerReceta(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inventarioService.obtenerRecetaFormateada(id));
        } catch (Exception e) {
            e.printStackTrace();
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error Nulo";
            String causa = e.getCause() != null ? e.getCause().getMessage() : "No hay detalles adicionales";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", mensaje, "causa", causa));
        }
    }

    // =====================================================================
    // ENDPOINTS DE INSUMOS
    // =====================================================================

    @GetMapping("/insumos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<Map<String, Object>>> listarInsumos(@RequestParam(required = false) Long sedeId) {
        Long sedeEfectiva = sedeId != null ? sedeId : TenantContext.getCurrentSede();
        return ResponseEntity.ok(inventarioService.listarInsumosConCosto(sedeEfectiva));
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Insumo> crearInsumo(@RequestBody InsumoRequestDTO dto) {
        Insumo i = new Insumo();
        i.setNombre(dto.getNombre());
        i.setUnidadMedida(dto.getUnidadMedida());
        i.setEmpresaId(TenantContext.getCurrentTenant()); 
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

    @PutMapping("/insumos/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Void> activarInsumo(@PathVariable Long id) {
        inventarioService.activarInsumo(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/insumos/stock-bajo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<List<InsumoBajoStockDTO>> listarInsumosConStockBajo(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(inventarioService.listarInsumosConStockBajo(sedeId));
    }

    // =====================================================================
    // 👇 ENDPOINTS DE KARDEX BLINDADOS EN MODO DEBUG 👇
    // =====================================================================

    @PostMapping("/kardex/entrada")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<?> registrarEntrada(@RequestBody EntradaAlmacenRequestDTO dto, Authentication auth) {
        try {
            inventarioService.registrarEntrada(dto, obtenerUsuario(auth));
            return ResponseEntity.ok().body(Map.of("message", "Entrada registrada exitosamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Fallo SQL: " + e.getMessage()));
        }
    }

    @PostMapping("/kardex/merma")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE', 'COCINA')")
    public ResponseEntity<?> registrarMerma(@RequestBody MermaRequestDTO dto, Authentication auth) {
        try {
            inventarioService.registrarMerma(dto, obtenerUsuario(auth));
            return ResponseEntity.ok().body(Map.of("message", "Merma registrada exitosamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Fallo SQL: " + e.getMessage()));
        }
    }

    @PostMapping("/kardex/ajuste")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA', 'GERENTE_SEDE')")
    public ResponseEntity<?> registrarAjuste(@RequestBody AjusteInventarioRequestDTO dto, Authentication auth) {
        try {
            inventarioService.registrarAjuste(dto, obtenerUsuario(auth));
            return ResponseEntity.ok().body(Map.of("message", "Ajuste registrado exitosamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Fallo SQL: " + e.getMessage()));
        }
    }

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