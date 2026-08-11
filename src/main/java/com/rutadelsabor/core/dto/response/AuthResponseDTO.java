package com.rutadelsabor.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String correo;
    private String rol;
    private Long empresaId;
    private List<String> modulosHabilitados;
    private String estadoSuscripcion;
}
