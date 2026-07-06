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
        // 1. Descubrir a qué empresa pertenece el usuario saltando a Hibernate
        Long empresaId = usuarioRepository.findEmpresaIdByCorreo(correo);
        
        if (empresaId == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
        }

        // 2. CORREGIDO: Pasamos el Long directamente porque tu TenantContext usa números
        TenantContext.setCurrentTenant(empresaId);

        try {
            // 3. Ahora sí, cargar la entidad completa. Usamos findByCorreoYEmpresa (nativa) en vez de
            // findByCorreo porque esta última pasa por el filtro @TenantId de Hibernate, cuyo valor
            // queda fijado en -1 al abrir la Session (spring.jpa.open-in-view) antes de que el
            // TenantContext.setCurrentTenant() de la línea anterior surta efecto.
            Usuario usuario = usuarioRepository.findByCorreoYEmpresa(correo, empresaId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            
            return new UserDetailsImpl(usuario);
        } finally {
            // 4. MUY IMPORTANTE: Limpiar el contexto para no dejar la empresa pegada en este hilo
            TenantContext.clear();
        }
    }
}