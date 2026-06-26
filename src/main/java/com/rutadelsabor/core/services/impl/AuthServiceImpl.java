package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.LoginRequestDTO;
import com.rutadelsabor.core.dto.response.AuthResponseDTO;
import com.rutadelsabor.core.security.JwtProvider;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IAuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AuthResponseDTO autenticarUsuario(LoginRequestDTO loginRequest) {
        // 1. Validar credenciales contra la base de datos (BCrypt)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getPassword()));

        // 2. Establecer el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Extraer el Principal ANTES de generar el token para obtener empresaId y usuarioId
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalArgumentException("El proceso de autenticación no generó un principal válido");
        }

        // 4. Generar el Token JWT usando la firma actualizada del JwtProvider
        String jwt = jwtProvider.generateToken(authentication, userDetails.getEmpresaId(), userDetails.getUsuarioId());

        String rol = userDetails.getAuthorities().iterator().next().getAuthority();

        return new AuthResponseDTO(jwt, userDetails.getUsername(), rol, userDetails.getEmpresaId());
    }
}