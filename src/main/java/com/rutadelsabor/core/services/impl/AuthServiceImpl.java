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

    // 1. Variables inmutables sin @Autowired de campo (Resuelve java:S6813)
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    // 2. Inyección por Constructor: Consistencia absoluta en el Core del sistema
    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AuthResponseDTO autenticarUsuario(LoginRequestDTO loginRequest) {
        // 1. Validar credenciales contra la base de datos (BCrypt)
        // Si falla, Spring Security lanza una excepción y nunca pasa de aquí
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getPassword()));

        // 2. Establecer el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generar el Token JWT con el empresa_id inyectado
        String jwt = jwtProvider.generateJwtToken(authentication);

        // 4. Programación Defensiva Optimizada (Resuelve java:S2589)
        // Ya no validamos si authentication es null, solo evaluamos el tipo del Principal
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalArgumentException("El proceso de autenticación no generó un principal válido");
        }

        String rol = userDetails.getAuthorities().iterator().next().getAuthority();

        return new AuthResponseDTO(jwt, userDetails.getUsername(), rol, userDetails.getEmpresaId());
    }
}