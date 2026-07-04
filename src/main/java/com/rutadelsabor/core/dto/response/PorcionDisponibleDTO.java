package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PorcionDisponibleDTO {
    private Long productoId;
    private String producto;
    // null si el producto no tiene receta cargada
    private BigDecimal porcionesDisponibles;
    private String estadoDisponibilidad;
}
