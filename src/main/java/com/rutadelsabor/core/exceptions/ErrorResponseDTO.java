package com.rutadelsabor.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// `codigo` distingue errores 403 entre sí:
//   ACCESO_DENEGADO      → @PreAuthorize falló (rol insuficiente)
//   MODULO_NO_HABILITADO → @RequiereModulo falló (plan no incluye módulo)
//   SUSCRIPCION_VENCIDA  → módulo core en solo lectura, operación de escritura denegada
//   null                 → cualquier otro error
@Getter
@Setter
@AllArgsConstructor
public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String codigo;
}
