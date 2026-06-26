package com.rutadelsabor.core.security;

import com.rutadelsabor.core.config.SseEmitterManager;
import com.rutadelsabor.core.config.tenant.TenantInterceptor;
import com.rutadelsabor.core.controllers.PedidoController;
import com.rutadelsabor.core.interceptors.ModuloInterceptor;
import com.rutadelsabor.core.repositories.SuscripcionRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IPedidoService;
import com.rutadelsabor.core.services.reportes.TicketManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de autorización por rol para PedidoController.
 * Cubre la matriz completa y verifica R1-1: ROLE_COCINA excluido de crear/confirmar/entregar.
 *
 * Las pruebas "bloqueado" verifican exactamente 403.
 * Las pruebas "permitido" verifican que la respuesta NO sea 403 (la capa de autorización pasó).
 */
@WebMvcTest(PedidoController.class)
@DisplayName("PedidoController — Matriz de autorización por rol")
class PedidoControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    // --- Controller dependencies ---
    @MockitoBean IPedidoService pedidoService;
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean TicketManager ticketManager;

    // --- Filter chain dependencies (no JWT en tests, pero el bean debe existir) ---
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserDetailsService userDetailsService;

    // --- Interceptor mocks: preHandle debe devolver true para no bloquear la solicitud ---
    @MockitoBean TenantInterceptor tenantInterceptor;
    @MockitoBean ModuloInterceptor moduloInterceptor;

    // SuscripcionRepository: requerido si ModuloInterceptor real es instanciado en algún contexto
    @MockitoBean SuscripcionRepository suscripcionRepository;

    @BeforeEach
    void setupInterceptors() throws Exception {
        when(tenantInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(moduloInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        // Stub mínimo para endpoints que llaman a usuarioRepository.findByCorreo()
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        // Stub listados para que devuelvan colecciones vacías y no NPE
        when(pedidoService.listarPedidosActivos()).thenReturn(Collections.emptyList());
        when(pedidoService.listarHistorial(any(), any())).thenReturn(Collections.emptyList());
    }

    // =========================================================================
    // R1-1 (CRÍTICA): POST /api/pedidos — COCINA debe recibir 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("R1-1: COCINA bloqueado en POST /api/pedidos")
    void cocina_bloqueado_crearPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en POST /api/pedidos")
    void mozo_permitido_crearPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_MOZO no debe recibir 403 en POST /api/pedidos"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en POST /api/pedidos")
    void cajero_bloqueado_crearPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE permitido en POST /api/pedidos")
    void gerente_permitido_crearPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_GERENTE no debe recibir 403 en POST /api/pedidos"));
    }

    // =========================================================================
    // R1-1 (CRÍTICA): PUT /api/pedidos/{id}/confirmar — COCINA debe recibir 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("R1-1: COCINA bloqueado en PUT /api/pedidos/{id}/confirmar")
    void cocina_bloqueado_confirmarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/confirmar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en PUT /api/pedidos/{id}/confirmar")
    void mozo_permitido_confirmarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/confirmar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/pedidos/{id}/confirmar")
    void cajero_bloqueado_confirmarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/confirmar"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // R1-1 (CRÍTICA): PUT /api/pedidos/{id}/entregar — COCINA debe recibir 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("R1-1: COCINA bloqueado en PUT /api/pedidos/{id}/entregar")
    void cocina_bloqueado_entregarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/entregar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en PUT /api/pedidos/{id}/entregar")
    void mozo_permitido_entregarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/entregar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA bloqueado en PUT /api/pedidos/{id}/entregar")
    void cocina_bloqueado_entregar_confirmado() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/entregar"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/pedidos/{id}/pagar — solo CAJERO, GERENTE, SUPER_ADMIN
    // =========================================================================

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en POST /api/pedidos/{id}/pagar")
    void mozo_bloqueado_procesarPago() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/pagar")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA bloqueado en POST /api/pedidos/{id}/pagar")
    void cocina_bloqueado_procesarPago() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/pagar")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO permitido en POST /api/pedidos/{id}/pagar")
    void cajero_permitido_procesarPago() throws Exception {
        // El cast a UserDetailsImpl fallará → 500, pero NO 403: autorización pasó.
        mockMvc.perform(post("/api/pedidos/1/pagar")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_CAJERO no debe recibir 403 en POST /api/pedidos/{id}/pagar"));
    }

    // =========================================================================
    // GET /api/pedidos/activos — COCINA incluido (necesita ver pedidos activos)
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en GET /api/pedidos/activos")
    void cocina_permitido_listarActivos() throws Exception {
        mockMvc.perform(get("/api/pedidos/activos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en GET /api/pedidos/activos")
    void mozo_permitido_listarActivos() throws Exception {
        mockMvc.perform(get("/api/pedidos/activos"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GET /api/pedidos/historial — solo CAJERO, GERENTE, SUPER_ADMIN
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA bloqueado en GET /api/pedidos/historial")
    void cocina_bloqueado_historial() throws Exception {
        mockMvc.perform(get("/api/pedidos/historial")
                        .param("inicio", "2025-01-01")
                        .param("fin", "2025-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en GET /api/pedidos/historial")
    void mozo_bloqueado_historial() throws Exception {
        mockMvc.perform(get("/api/pedidos/historial")
                        .param("inicio", "2025-01-01")
                        .param("fin", "2025-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO permitido en GET /api/pedidos/historial")
    void cajero_permitido_historial() throws Exception {
        mockMvc.perform(get("/api/pedidos/historial")
                        .param("inicio", "2025-01-01")
                        .param("fin", "2025-12-31"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // PUT /api/pedidos/{id}/cancelar — solo GERENTE, SUPER_ADMIN
    // =========================================================================

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA bloqueado en PUT /api/pedidos/{id}/cancelar")
    void cocina_bloqueado_cancelar() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en PUT /api/pedidos/{id}/cancelar")
    void mozo_bloqueado_cancelar() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/pedidos/{id}/cancelar")
    void cajero_bloqueado_cancelar() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE permitido en PUT /api/pedidos/{id}/cancelar")
    void gerente_permitido_cancelar() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelar"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // PUT /api/pedidos/{id}/descuento — solo GERENTE, SUPER_ADMIN
    // =========================================================================

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/pedidos/{id}/descuento")
    void cajero_bloqueado_descuento() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/descuento").param("monto", "5.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO bloqueado en PUT /api/pedidos/{id}/descuento")
    void mozo_bloqueado_descuento() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/descuento").param("monto", "5.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("GERENTE permitido en PUT /api/pedidos/{id}/descuento")
    void gerente_permitido_descuento() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/descuento").param("monto", "5.00"))
                .andExpect(status().isOk());
    }
}
