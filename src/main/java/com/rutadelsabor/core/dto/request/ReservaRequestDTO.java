package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReservaRequestDTO {
    private String nombreCliente;
    private String telefonoCliente;
    private LocalDateTime fechaHora;
    private Integer cantidadPersonas;
    private String notas;
    private Long sedeId;
}