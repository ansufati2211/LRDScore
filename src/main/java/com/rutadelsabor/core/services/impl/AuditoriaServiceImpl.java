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

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaServiceImpl(AuditoriaLogRepository auditoriaLogRepository, 
                                UsuarioRepository usuarioRepository) {
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccion(Long usuarioId, Long empresaId, String accion, String modulo, String detalles) {
        AuditoriaLog log = new AuditoriaLog();
        
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            log.setUsuario(usuario);
        }

        log.setEmpresaId(empresaId); 
        log.setAccion(accion);
        log.setModulo(modulo);
        log.setDetalle(detalles); 
        
        auditoriaLogRepository.save(log);
    }
}