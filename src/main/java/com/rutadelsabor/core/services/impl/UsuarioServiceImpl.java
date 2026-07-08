package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.models.entities.UsuarioSede;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.repositories.UsuarioSedeRepository;
import com.rutadelsabor.core.services.interfaces.IUsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    // FIX SonarLint: Constante para evitar duplicación de texto
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioSedeRepository usuarioSedeRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, 
                              UsuarioSedeRepository usuarioSedeRepository, 
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioSedeRepository = usuarioSedeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario crearUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new ReglaNegocioException("El correo ya está registrado en el sistema.");
        }
        
        Long empresaId = TenantContext.getCurrentTenant();
        
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(dto.getRol().toUpperCase());
        usuario.setEmpresaId(empresaId);

        Usuario savedUser = usuarioRepository.save(usuario);

        if (dto.getSedeId() != null) {
            UsuarioSede us = new UsuarioSede();
            us.setUsuarioId(savedUser.getId());
            us.setSedeId(dto.getSedeId());
            us.setEmpresaId(empresaId);
            usuarioSedeRepository.save(us);
        } else if (!dto.getRol().equals("ROLE_ADMIN_EMPRESA") && !dto.getRol().equals("ROLE_SUPER_ADMIN")) {
            throw new ReglaNegocioException("Debe asignar obligatoriamente un Local (sedeId) a un rol operativo.");
        }

        return savedUser;
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        if (dto.getNombre() != null) u.setNombre(dto.getNombre());
        if (dto.getRol() != null) u.setRol(dto.getRol().toUpperCase());
        return usuarioRepository.save(u);
    }

    // FIX Error 1: Implementación del método faltante
    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordDTO dto) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        if (!passwordEncoder.matches(dto.getPasswordActual(), u.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual es incorrecta");
        }
        u.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuarioRepository.save(u);
    }

    // FIX Error 2: Implementación del método faltante
    @Override
    @Transactional
    public void resetPassword(Long id, String nuevaPassword) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        u.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(u);
    }

    @Override
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        u.setEstadoRegistro(false);
        usuarioRepository.save(u);
    }

    @Override
    @Transactional
    public void activarUsuario(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
        u.setEstadoRegistro(true);
        usuarioRepository.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }
}