package com.rutadelsabor.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);

    // FASE 7: Indexado por (empresaId, usuarioId)
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, UserSseSession>> tenants = new ConcurrentHashMap<>();

    static final class UserSseSession {
        final String rol;
        final Long sedeId; // <-- FASE 7: Aislamiento por local físico
        final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        UserSseSession(String rol, Long sedeId) {
            this.rol = rol;
            this.sedeId = sedeId;
        }
    }

    public SseEmitter suscribir(Long empresaId, Long usuarioId, String rol, Long sedeId) {
        SseEmitter emitter = new SseEmitter(120000L); // 2 minutos de timeout
        
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.computeIfAbsent(empresaId, k -> new ConcurrentHashMap<>());
        UserSseSession session = empresa.computeIfAbsent(usuarioId, k -> new UserSseSession(rol, sedeId));
        session.emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(empresaId, usuarioId, emitter));
        emitter.onTimeout(() -> removeEmitter(empresaId, usuarioId, emitter));
        emitter.onError(e -> removeEmitter(empresaId, usuarioId, emitter));

        return emitter;
    }

    private void removeEmitter(Long empresaId, Long usuarioId, SseEmitter emitter) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        empresa.computeIfPresent(usuarioId, (k, session) -> {
            session.emitters.remove(emitter);
            return session.emitters.isEmpty() ? null : session;
        });
        if (empresa.isEmpty()) tenants.remove(empresaId);
    }

    private void enviar(List<SseEmitter> emitters, String evento, Object datos) {
        List<SseEmitter> muertos = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(datos, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                log.warn("SSE emitter desconectado, eliminando.");
                muertos.add(emitter);
            }
        }
        emitters.removeAll(muertos);
    }

    // Evento Global a la Empresa (Ej: Reset de Stock)
    public void publicarTenant(Long empresaId, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        for (UserSseSession session : empresa.values()) {
            enviar(session.emitters, evento, datos);
        }
    }

    // FASE 7: Evento Aislado a una Sede Específica (Ej: Nuevo Pedido en Ica)
    public void publicarTenantYSede(Long empresaId, Long sedeId, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        for (UserSseSession session : empresa.values()) {
            // Si la sesión no tiene sede (es el ADMIN global) o coincide la sede
            if (session.sedeId == null || session.sedeId.equals(sedeId)) {
                enviar(session.emitters, evento, datos);
            }
        }
    }

    // FASE 7: Evento Aislado por Rol y Sede (Ej: Alerta Demora solo a COCINA de Ica)
    public void publicarPorRolYSede(Long empresaId, String rol, Long sedeId, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        for (UserSseSession session : empresa.values()) {
            boolean rolCoincide = session.rol.equals(rol);
            boolean sedeCoincide = session.sedeId == null || session.sedeId.equals(sedeId);
            if (rolCoincide && sedeCoincide) {
                enviar(session.emitters, evento, datos);
            }
        }
    }
}