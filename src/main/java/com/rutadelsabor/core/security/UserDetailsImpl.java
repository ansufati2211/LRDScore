package com.rutadelsabor.core.security;

import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.enums.RolUsuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    // 1. Definimos constantes básicas serializables en lugar de la Entidad JPA pesada (Resuelve java:S1948)
    private final Long usuarioId;
    private final Long empresaId;
    private final String correo;
    private final String passwordHash;
    private final RolUsuario rol;
    private final Boolean activo;

    // 2. Constructor personalizado que se encarga de mapear y desvincular la Entidad JPA
    public UserDetailsImpl(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.empresaId = usuario.getEmpresaId();
        this.correo = usuario.getCorreo();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol();
        this.activo = usuario.getEstadoRegistro();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Extraemos el nombre del enumerado del rol (ej. ROLE_CAJERO)
        return Collections.singletonList(new SimpleGrantedAuthority(rol.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.correo; // El correo electrónico opera como el identificador de inicio de sesión
    }

    // Métodos limpios inyectados de manera directa en el Token JWT
    public Long getEmpresaId() {
        return this.empresaId;
    }

    public Long getUsuarioId() {
        return this.usuarioId;
    }

    @Override
    public boolean isAccountNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isAccountNonLocked() { 
        return true; 
    }
    
    @Override
    public boolean isCredentialsNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isEnabled() { 
        return Boolean.TRUE.equals(this.activo); 
    }
}