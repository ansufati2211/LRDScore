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
    private final Long sedeId;
    private final String correo;
    private final String passwordHash;
    private final String rol;
    private final Boolean activo;

    public UserDetailsImpl(Usuario usuario, Long sedeId) {
        this.usuarioId = usuario.getId();
        this.empresaId = usuario.getEmpresaId();
        this.sedeId = sedeId; 
        this.correo = usuario.getCorreo();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol(); 
        this.activo = usuario.getEstadoRegistro();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(rol));
    }
    @Override public String getPassword() { return this.passwordHash; }
    @Override public String getUsername() { return this.correo; }
    public Long getEmpresaId() { return this.empresaId; }
    public Long getUsuarioId() { return this.usuarioId; }
    public Long getSedeId() { return this.sedeId; }
    public String getRol() { return this.rol; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return Boolean.TRUE.equals(this.activo); }
}