package com.rutadelsabor.core.interceptors;

import com.rutadelsabor.core.annotations.RequiereModulo;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.ModuloNoHabilitadoException;
import com.rutadelsabor.core.exceptions.SuscripcionVencidaException;
import com.rutadelsabor.core.models.entities.Suscripcion;
import com.rutadelsabor.core.models.enums.EstadoSuscripcion;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.repositories.SuscripcionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class ModuloInterceptor implements HandlerInterceptor {

    private final SuscripcionRepository suscripcionRepository;

    public ModuloInterceptor(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    @Override
    @SuppressWarnings("java:S3516") 
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiereModulo annotation = handlerMethod.getMethodAnnotation(RequiereModulo.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequiereModulo.class);
        }

        if (annotation == null) {
            return true;
        }

        Modulo modulo = annotation.value();
        Long empresaId = TenantContext.getCurrentTenant();

        if (empresaId == null) {
            throw new ModuloNoHabilitadoException(modulo);
        }

        Suscripcion suscripcion = suscripcionRepository
                .findFirstByEmpresaIdAndEstadoInOrderByFechaInicioDesc(
                        empresaId,
                        List.of(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.VENCIDA))
                .orElseThrow(() -> new ModuloNoHabilitadoException(modulo));

        EstadoSuscripcion estado = suscripcion.getEstado();

        if (estado == EstadoSuscripcion.ACTIVA) {
            verificarModuloEnPlan(suscripcion, modulo);
            return true; 
        }

        if (estado == EstadoSuscripcion.VENCIDA) {
            verificarVencida(request, modulo);
            return true; 
        }

        throw new ModuloNoHabilitadoException(modulo);
    }

    private void verificarModuloEnPlan(Suscripcion suscripcion, Modulo modulo) {
        boolean habilitado = suscripcion.getPlan().getModulos().stream()
                .anyMatch(pm -> pm.getCodigoModulo() == modulo);

        if (!habilitado) {
            throw new ModuloNoHabilitadoException(modulo);
        }
    }

    private void verificarVencida(HttpServletRequest request, Modulo modulo) {
        if (!modulo.esCore()) {
            throw new ModuloNoHabilitadoException(modulo);
        }
        String method = request.getMethod();
        boolean esLectura = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);

        if (!esLectura) {
            throw new SuscripcionVencidaException(modulo);
        }
    }
}