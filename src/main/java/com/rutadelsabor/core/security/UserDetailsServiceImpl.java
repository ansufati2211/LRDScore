package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.config.tenant.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Long empresaId = usuarioRepository.findEmpresaIdByCorreo(correo);
        if (empresaId == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
        }

        TenantContext.setCurrentTenant(empresaId);

        try {
            Usuario usuario = usuarioRepository.findByCorreoYEmpresa(correo, empresaId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            
            // Integración Multi-Sede
            Long sedeId = usuarioRepository.findPrimerSedeIdByUsuarioId(usuario.getId()).orElse(null);
            
            return new UserDetailsImpl(usuario, sedeId);
        } finally {
            TenantContext.clear();
        }
    }
}