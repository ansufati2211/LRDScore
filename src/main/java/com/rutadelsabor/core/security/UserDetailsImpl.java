package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final Usuario usuario;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Extraemos el rol (ej. ROLE_CAJERO) y lo convertimos en un GrantedAuthority
        return Collections.singletonList(new SimpleGrantedAuthority(usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo(); // El correo será el identificador de login
    }

    // Métodos extraídos para inyectarlos en el Token JWT luego
    public Long getEmpresaId() {
        return usuario.getEmpresaId();
    }

    public Long getUsuarioId() {
        return usuario.getId();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return usuario.getEstadoRegistro(); }
}