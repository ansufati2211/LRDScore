package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.models.entities.Usuario;

import java.util.List;

public interface IUsuarioService {
    Usuario crearUsuario(UsuarioRequestDTO dto);
    Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto);
    Usuario obtenerUsuario(Long id); // FIX Error 1
    void cambiarPassword(Long id, CambiarPasswordDTO dto);
    void resetPassword(Long id, String nuevaPassword); // FIX Error 2
    void desactivarUsuario(Long id);
    void activarUsuario(Long id); // FIX Error 4
List<Usuario> listarUsuarios(Long sedeId);
}