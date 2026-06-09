package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // 1. Declaramos la dependencia como final e inmutable (Resuelve java:S6813)
    private final UsuarioRepository usuarioRepository;

    // 2. Inyección por Constructor
    // Spring Boot inyectará el repositorio automáticamente sin necesidad de usar @Autowired
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        return new UserDetailsImpl(usuario);
    }
}