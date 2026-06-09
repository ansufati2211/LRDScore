package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.models.entities.AuditoriaLog;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.AuditoriaLogRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IAuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaServiceImpl implements IAuditoriaService {

    // 1. Declaramos las dependencias como finales e inmutables (Resuelve java:S6813)
    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;

    // 2. Inyección por Constructor
    // Al tener un único constructor, Spring Boot resuelve e inyecta los beans de forma automática.
    public AuditoriaServiceImpl(AuditoriaLogRepository auditoriaLogRepository, 
                                UsuarioRepository usuarioRepository) {
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    // Propagation.REQUIRES_NEW asegura que si una transacción de negocio falla y hace rollback, 
    // el log de auditoría SÍ se guarde de manera independiente en la base de datos de la sede.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccion(Long usuarioId, Long empresaId, String accion, String modulo, String detalles) {
        AuditoriaLog log = new AuditoriaLog();
        
        // Buscamos el objeto Usuario para asociar la relación ManyToOne de forma íntegra
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            log.setUsuario(usuario);
        }

        log.setEmpresaId(empresaId); // Campo heredado desde BaseTenantEntity
        log.setAccion(accion);
        log.setModulo(modulo);
        log.setDetalle(detalles); // Respeta el nombre en singular mapeado en tu Entidad
        
        auditoriaLogRepository.save(log);
    }
}