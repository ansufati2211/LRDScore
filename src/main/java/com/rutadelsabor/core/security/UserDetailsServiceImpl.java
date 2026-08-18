package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Empresa;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.EmpresaRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.config.tenant.TenantContext;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Long empresaId = usuarioRepository.findEmpresaIdByCorreo(correo);
        if (empresaId == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa != null && Boolean.FALSE.equals(empresa.getEstadoRegistro())) {
            throw new DisabledException("ACCESO DENEGADO: La empresa se encuentra suspendida por falta de pago.");
        }

        TenantContext.setCurrentTenant(empresaId);

        try {
            Usuario usuario = usuarioRepository.findByCorreoYEmpresa(correo, empresaId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inhabilitado"));
            
            Long sedeId = usuarioRepository.findPrimerSedeIdByUsuarioId(usuario.getId()).orElse(null);
            
            return new UserDetailsImpl(usuario, sedeId);
        } finally {
            TenantContext.clear();
        }
    }
}