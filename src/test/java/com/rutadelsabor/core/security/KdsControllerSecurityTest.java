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

@WebMvcTest(KdsController.class)
@DisplayName("KdsController — Matriz de autorización por rol Multi-Sede")
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
    void setup() {
        when(tenantInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
        when(moduloInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
                
        // FIX: Agregado any() para simular el nuevo parámetro sedeIdFiltro
        when(kdsService.obtenerPedidosPendientes(any())).thenReturn(Collections.emptyList());
        
        // FIX: Agregado any() para simular el nuevo parámetro sedeId en la suscripción SSE
        when(sseEmitterManager.suscribir(any(), any(), any(), any())).thenReturn(new SseEmitter());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en GET /api/v1/kds/pendientes")
    void cocina_permitido_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/v1/kds/pendientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en GET /api/v1/kds/pendientes")
    void cajero_bloqueado_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/v1/kds/pendientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_EMPRESA")
    @DisplayName("ADMIN_EMPRESA permitido en GET /api/v1/kds/pendientes")
    void admin_permitido_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/v1/kds/pendientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE_SEDE")
    @DisplayName("GERENTE_SEDE permitido en GET /api/v1/kds/pendientes")
    void gerente_sede_permitido_obtenerPendientes() throws Exception {
        mockMvc.perform(get("/api/v1/kds/pendientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en PUT /api/v1/kds/{id}/preparando")
    void cocina_permitido_marcarPreparando() throws Exception {
        mockMvc.perform(put("/api/v1/kds/1/preparando"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus(),
                                "ROLE_COCINA no debe recibir 403 en PUT /api/v1/kds/{id}/preparando"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    @DisplayName("CAJERO bloqueado en PUT /api/v1/kds/{id}/preparando")
    void cajero_bloqueado_marcarPreparando() throws Exception {
        mockMvc.perform(put("/api/v1/kds/1/preparando"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COCINA")
    @DisplayName("COCINA permitido en PUT /api/v1/kds/{id}/listo")
    void cocina_permitido_marcarListo() throws Exception {
        mockMvc.perform(put("/api/v1/kds/1/listo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_EMPRESA")
    @DisplayName("ADMIN_EMPRESA permitido en GET /api/v1/kds/eventos")
    void admin_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/v1/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @WithMockUser(roles = "GERENTE_SEDE")
    @DisplayName("GERENTE_SEDE permitido en GET /api/v1/kds/eventos")
    void gerente_sede_permitido_eventos() throws Exception {
        mockMvc.perform(get("/api/v1/kds/eventos"))
                .andExpect(result ->
                        assertNotEquals(403, result.getResponse().getStatus()));
    }
}