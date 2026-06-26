package com.rutadelsabor.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// R0-3: incluye modulosHabilitados para que el frontend oculte/deshabilite la UI correspondiente.
// estadoSuscripcion permite al frontend mostrar el banner de aviso cuando es VENCIDA (E0-1).
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
