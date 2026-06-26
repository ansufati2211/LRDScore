package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Long usuarioId;
    private final Long empresaId;
    private final String correo;
    private final String passwordHash;
    private final String rol; // FIX: Ahora es String, coincidiendo con la Entidad
    private final Boolean activo;

    public UserDetailsImpl(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.empresaId = usuario.getEmpresaId();
        this.correo = usuario.getCorreo();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol(); // FIX: Asignación directa y segura
        this.activo = usuario.getEstadoRegistro();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // FIX: El rol ya es un String (ej. "ROLE_CAJERO"), lo pasamos directo
        return Collections.singletonList(new SimpleGrantedAuthority(rol));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.correo;
    }

    public Long getEmpresaId() {
        return this.empresaId;
    }

    public Long getUsuarioId() {
        return this.usuarioId;
    }

    public String getRol() {
        return this.rol;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return Boolean.TRUE.equals(this.activo); }
}