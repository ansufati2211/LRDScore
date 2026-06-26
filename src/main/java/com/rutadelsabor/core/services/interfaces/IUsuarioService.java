package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.CambiarPasswordDTO;
import com.rutadelsabor.core.dto.request.UsuarioRequestDTO;
import com.rutadelsabor.core.models.entities.Usuario;

import java.util.List;

public interface IUsuarioService {
    List<Usuario> listarUsuarios();
    Usuario obtenerUsuario(Long id);
    Usuario crearUsuario(UsuarioRequestDTO dto);
    Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto);
    void desactivarUsuario(Long id);
    void cambiarPassword(Long id, CambiarPasswordDTO dto);

    void resetPassword(Long id, String nuevaPassword);
}
