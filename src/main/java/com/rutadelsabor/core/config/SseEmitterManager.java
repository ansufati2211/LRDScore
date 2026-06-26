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

    // R2-1 (Invariante I-1): indexado por (empresaId, usuarioId) — ningún evento puede cruzar
    // fronteras de tenant. El empresaId siempre proviene del token validado, nunca de parámetro externo.
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, UserSseSession>> tenants =
            new ConcurrentHashMap<>();

    static final class UserSseSession {
        final String rol;
        final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        UserSseSession(String rol) {
            this.rol = rol;
        }
    }

    /**
     * Registra un emitter bajo (empresaId, usuarioId). Múltiples conexiones del mismo usuario
     * (tabs del navegador) acumulan emitters en la misma sesión.
     */
    public SseEmitter suscribir(Long empresaId, Long usuarioId, String rol) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        UserSseSession session = tenants
                .computeIfAbsent(empresaId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(usuarioId, k -> new UserSseSession(rol));
        session.emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(empresaId, usuarioId, emitter));
        emitter.onTimeout(() -> removeEmitter(empresaId, usuarioId, emitter));
        emitter.onError(e -> removeEmitter(empresaId, usuarioId, emitter));
        return emitter;
    }

    // Broadcast a todos los emisores del tenant (COCINA + MOZO + otros conectados al tenant)
    public void publicarTenant(Long empresaId, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        for (UserSseSession session : empresa.values()) {
            enviar(session.emitters, evento, datos);
        }
    }

    // Solo al usuario específico dentro del tenant
    public void publicarUsuario(Long empresaId, Long usuarioId, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        UserSseSession session = empresa.get(usuarioId);
        if (session != null) {
            enviar(session.emitters, evento, datos);
        }
    }

    // A todos los usuarios con el rol indicado dentro del tenant
    public void publicarPorRol(Long empresaId, String rol, String evento, Object datos) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        for (UserSseSession session : empresa.values()) {
            if (rol.equals(session.rol)) {
                enviar(session.emitters, evento, datos);
            }
        }
    }

    private void enviar(List<SseEmitter> emitters, String evento, Object datos) {
        List<SseEmitter> muertos = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(datos, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                log.warn("SSE emitter desconectado, eliminando: {}", e.getMessage());
                muertos.add(emitter);
            }
        }
        emitters.removeAll(muertos);
    }

    private void removeEmitter(Long empresaId, Long usuarioId, SseEmitter emitter) {
        ConcurrentHashMap<Long, UserSseSession> empresa = tenants.get(empresaId);
        if (empresa == null) return;
        // computeIfPresent es atómico: elimina la sesión solo si quedó sin emitters
        empresa.computeIfPresent(usuarioId, (k, session) -> {
            session.emitters.remove(emitter);
            return session.emitters.isEmpty() ? null : session;
        });
        // Elimina la entrada del tenant si no quedan sesiones (evita fuga de memoria con N tenants)
        tenants.computeIfPresent(empresaId, (k, e) -> e.isEmpty() ? null : e);
    }
}
