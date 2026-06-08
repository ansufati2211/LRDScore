package com.rutadelsabor.core.services.interfaces;

import com.rutadelsabor.core.dto.request.LoginRequestDTO;
import com.rutadelsabor.core.dto.response.AuthResponseDTO;

public interface IAuthService {
    AuthResponseDTO autenticarUsuario(LoginRequestDTO loginRequest);
}