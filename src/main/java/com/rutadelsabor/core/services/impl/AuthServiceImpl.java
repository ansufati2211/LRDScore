package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.dto.request.LoginRequestDTO;
import com.rutadelsabor.core.dto.response.AuthResponseDTO;
import com.rutadelsabor.core.security.JwtProvider;
import com.rutadelsabor.core.security.UserDetailsImpl;
import com.rutadelsabor.core.services.interfaces.IAuthService;
import com.rutadelsabor.core.services.interfaces.ISuscripcionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final ISuscripcionService suscripcionService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtProvider jwtProvider,
                           ISuscripcionService suscripcionService) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.suscripcionService = suscripcionService;
    }

    // R0-1 se cumple en conjunto con ModuloInterceptor y @PreAuthorize.
    // R0-3: el response incluye modulosHabilitados y estadoSuscripcion.
    @Override
    public AuthResponseDTO autenticarUsuario(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalArgumentException("El proceso de autenticación no generó un principal válido");
        }

        // FIX: Se extraen las 3 variables necesarias del principal
        Long empresaId = userDetails.getEmpresaId();
        Long usuarioId = userDetails.getUsuarioId();
        Long sedeId = userDetails.getSedeId(); // Variable añadida de la Fase 1

        // FIX: Se pasan los 4 argumentos completos, resolviendo el error de compilación
        String jwt = jwtProvider.generateToken(authentication, empresaId, usuarioId, sedeId);
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();

        // R0-3: módulos del plan vigente; E0-1: si VENCIDA devuelve solo módulos core.
        List<String> modulosHabilitados = suscripcionService.obtenerModulosHabilitados(empresaId);
        String estadoSuscripcion = suscripcionService.obtenerSuscripcionVigente(empresaId)
                .map(s -> s.getEstado().name())
                .orElse("SIN_SUSCRIPCION");

        // Nota: El DTO sigue devolviendo los mismos datos al frontend por ahora
        return new AuthResponseDTO(jwt, userDetails.getUsername(), rol, empresaId,
                modulosHabilitados, estadoSuscripcion);
    }
}