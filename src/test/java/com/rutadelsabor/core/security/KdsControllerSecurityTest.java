package com.rutadelsabor.core.security;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantInterceptor;
import com.rutadelsabor.core.controllers.KdsController;
import com.rutadelsabor.core.interceptors.ModuloInterceptor;
import com.rutadelsabor.core.repositories.SuscripcionRepository;
import com.rutadelsabor.core.services.interfaces.IKdsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de autorización por rol para KdsController.
 *
 * Clase-nivel @PreAuthorize: SUPER_ADMIN, GERENTE, COCINA, MOZO, CAJERO (CAJERO añadido en R2)
 * Método-nivel @PreAuthorize en /pendientes, /preparando, /listo: SUPER_ADMIN, GERENTE, COCINA
 * MOZO y CAJERO pueden ver /eventos pero NO pueden cambiar estados de preparación.
 */
@WebMvcTest(KdsController.class)
@DisplayName("KdsController — Matriz de autorización por rol")
class KdsControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean IKdsService kdsService;
    @MockitoBean SseEmitterManager sseEmitterManager;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean TenantInterceptor tenantInterceptor;
    @MockitoBean ModuloInterceptor moduloInterceptor;
    @MockitoBean SuscripcionRepository suscripcionRepository;

   @BeforeEach
    void setup() { // <-- Cambio aquí: se eliminó 'throws Exception'
        when(tenantInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(moduloInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(kdsService.obtenerPedidosPendientes()).thenReturn(Collections.emptyList());
        // suscribir(Long empresaId, Long usuarioId, String rol) — firma actualizada en R2
        when(sseEmitterManager.suscribir(any(), any(), any())).thenReturn(new SseEmitter());
    }

    // =========================================================================
    // GET /api/kds/pendientes — SUPER_ADMIN, GERENTE, COCINA (MOZO excluido a nivel método)
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en GET /api/kds/pendientes")
    void cocina_permitido_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/kds/pendientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en GET /api/kds/pendientes (excluido a nivel método)")
    void cajero_bloqueado_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/kds/pendientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en GET /api/kds/pendientes (excluido a nivel método)")
    void mozo_bloqueado_kds_pendientes() throws Exception {
        mockMvc.perform(get("/api/kds/pendientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE permitido en GET /api/kds/pendientes")
    void gerente_permitido_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/kds/pendientes"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // PUT /api/kds/{id}/preparando — SUPER_ADMIN, GERENTE, COCINA
    // Nota: el controlador hace cast a UserDetailsImpl → puede lanzar ClassCastException (500),
    // pero eso ocurre DESPUÉS de que la autorización pasó: la prueba verifica "no es 403".
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en PUT /api/kds/{id}/preparando")
    void cocina_permitido_marcarPreparando() throws Exception {
        mockMvc.perform(put("/api/kds/1/preparando"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_COCINA no debe recibir 403 en PUT /api/kds/{id}/preparando"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/kds/{id}/preparando (excluido a nivel método)")
    void cajero_bloqueado_marcarPreparando() throws Exception {
        mockMvc.perform(put("/api/kds/1/preparando"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en PUT /api/kds/{id}/preparando (excluido a nivel método)")
    void mozo_bloqueado_marcarPreparando() throws Exception {
        mockMvc.perform(put("/api/kds/1/preparando"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // PUT /api/kds/{id}/listo — SUPER_ADMIN, GERENTE, COCINA
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en PUT /api/kds/{id}/listo")
    void cocina_permitido_marcarListo() throws Exception {
        mockMvc.perform(put("/api/kds/1/listo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/kds/{id}/listo (excluido a nivel método)")
    void cajero_bloqueado_marcarListo() throws Exception {
        mockMvc.perform(put("/api/kds/1/listo"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en PUT /api/kds/{id}/listo (excluido a nivel método)")
    void mozo_bloqueado_marcarListo() throws Exception {
        mockMvc.perform(put("/api/kds/1/listo"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/kds/eventos (SSE) — clase-nivel: SUPER_ADMIN, GERENTE, COCINA, MOZO, CAJERO
    // R2: CAJERO añadido para recibir escalación a t=2min.
    // El cast a UserDetailsImpl en el controlador produce 500 con @WithMockUser (no es UserDetailsImpl),
    // pero 500 ≠ 403, lo que demuestra que la autorización pasó.
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en GET /api/kds/eventos")
    void cocina_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_COCINA no debe recibir 403 en GET /api/kds/eventos"));
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en GET /api/kds/eventos (clase-nivel incluye MOZO)")
    void mozo_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_MOZO no debe recibir 403 en GET /api/kds/eventos"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("R2: CAJERO permitido en GET /api/kds/eventos (recibe escalación t=2min)")
    void cajero_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_CAJERO no debe recibir 403 en GET /api/kds/eventos — necesita SSE para escalación nivel 2"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE permitido en GET /api/kds/eventos (recibe escalación t=5min)")
    void gerente_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_GERENTE no debe recibir 403 en GET /api/kds/eventos"));
    }
}