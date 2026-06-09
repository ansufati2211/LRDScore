package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.LoginRequestDTO;
import com.rutadelsabor.core.dto.response.AuthResponseDTO;
import com.rutadelsabor.core.services.interfaces.IAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 1. Lo marcamos como final para mayor seguridad
    private final IAuthService authService;

    // 2. Inyección por Constructor (Resuelve java:S6813)
    // Ya no hace falta poner @Autowired, Spring Boot lo hace automáticamente 
    // cuando la clase tiene un solo constructor.
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.autenticarUsuario(loginRequest);
        return ResponseEntity.ok(response);
    }
}