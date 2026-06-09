package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteRequestDTO {
    private String nombreRazonSocial;
    private String tipoDocumento; // DNI, RUC, CE
    private String numeroDocumento;
    private String direccion;
    private String correo;
    private String telefono;
}