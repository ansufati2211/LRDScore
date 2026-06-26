package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Usuario;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import com.rutadelsabor.core.services.interfaces.IUsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public Usuario crearUsuario(UsuarioRequestDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria al crear un usuario.");
        }
        if (dto.getPassword().length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener mínimo 8 caracteres.");
        }
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new ReglaNegocioException("Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(dto.getRol());
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = obtenerUsuario(id);
        if (!usuario.getCorreo().equalsIgnoreCase(dto.getCorreo())
                && usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new ReglaNegocioException("Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setRol(dto.getRol());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8) {
                throw new ReglaNegocioException("La contraseña debe tener mínimo 8 caracteres.");
            }
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = obtenerUsuario(id);
        usuario.setEstadoRegistro(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank() || nuevaPassword.length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener mínimo 8 caracteres.");
        }
        Usuario usuario = obtenerUsuario(id);
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordDTO dto) {
        Usuario usuario = obtenerUsuario(id);
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }
}
