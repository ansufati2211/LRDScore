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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@SuppressWarnings("unused")
@WebMvcTest(PedidoController.class)
@DisplayName("PedidoController — Matriz de autorización por rol Multi-Sede")
class PedidoControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean IPedidoService pedidoService;
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean TicketManager ticketManager;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean TenantInterceptor tenantInterceptor;
    @MockitoBean ModuloInterceptor moduloInterceptor;
    @MockitoBean SuscripcionRepository suscripcionRepository;

    @BeforeEach
    void setupInterceptors() {
        when(tenantInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(moduloInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        
        // FIX: Se agrega un any() extra a los Mocks para que calcen con la nueva firma IPedidoService
        when(pedidoService.listarPedidosActivos(any())).thenReturn(Collections.emptyList());
        when(pedidoService.listarHistorial(any(), any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("R1-1: COCINA bloqueado en POST /api/v1/pedidos")
    void cocina_bloqueado_crearPedido() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_EMPRESA")
    @DisplayName("ADMIN_EMPRESA permitido en POST /api/v1/pedidos")
    void admin_permitido_crearPedido() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @WithMockUser(roles = "GERENTE_SEDE")
    @DisplayName("GERENTE_SEDE permitido en POST /api/v1/pedidos")
    void gerente_sede_permitido_crearPedido() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("R1-1: COCINA bloqueado en PUT /api/v1/pedidos/{id}/confirmar")
    void cocina_bloqueado_confirmarPedido() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/confirmar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOZO")
    @DisplayName("MOZO permitido en PUT /api/v1/pedidos/{id}/confirmar")
    void mozo_permitido_confirmarPedido() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/confirmar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_EMPRESA")
    @DisplayName("ADMIN_EMPRESA permitido en PUT /api/v1/pedidos/{id}/cancelar")
    void admin_permitido_cancelar() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/cancelar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE_SEDE")
    @DisplayName("GERENTE_SEDE permitido en PUT /api/v1/pedidos/{id}/cancelar")
    void gerente_sede_permitido_cancelar() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/cancelar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_EMPRESA")
    @DisplayName("ADMIN_EMPRESA permitido en PUT /api/v1/pedidos/{id}/descuento")
    void admin_permitido_descuento() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/descuento").param("monto", "5.00"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE_SEDE")
    @DisplayName("GERENTE_SEDE permitido en PUT /api/v1/pedidos/{id}/descuento")
    void gerente_sede_permitido_descuento() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/1/descuento").param("monto", "5.00"))
                .andExpect(status().isOk());
    }
}