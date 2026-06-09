package com.rutadelsabor.core.services.interfaces;

public interface IAuditoriaService {
    void registrarAccion(Long usuarioId, Long empresaId, String accion, String modulo, String detalles);
}