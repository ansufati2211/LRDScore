package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.dto.request.LoginRequestDTO;
import com.rutadelsabor.core.dto.response.AuthResponseDTO;
import com.rutadelsabor.core.services.interfaces.IAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.autenticarUsuario(loginRequest);
        return ResponseEntity.ok(response);
    }
}