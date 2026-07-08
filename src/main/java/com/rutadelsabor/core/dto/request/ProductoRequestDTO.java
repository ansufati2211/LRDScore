package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ProductoRequestDTO {
    private Long categoriaId;
    private String nombre;
    private BigDecimal precioVenta;
    private String tagsBusqueda;
    private Boolean esPreparado;
private Integer tiempoPreparacionMinutos;
}