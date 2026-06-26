package com.rutadelsabor.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene formato válido")
    private String correo;

    // Requerida al crear, opcional al actualizar (si es null/blank se mantiene la existente)
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(
        regexp = "ROLE_SUPER_ADMIN|ROLE_GERENTE|ROLE_CAJERO|ROLE_MOZO|ROLE_COCINA",
        message = "Rol inválido. Valores permitidos: ROLE_SUPER_ADMIN, ROLE_GERENTE, ROLE_CAJERO, ROLE_MOZO, ROLE_COCINA"
    )
    private String rol;
}
