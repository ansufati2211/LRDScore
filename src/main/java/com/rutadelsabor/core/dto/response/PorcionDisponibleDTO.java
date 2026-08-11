package com.rutadelsabor.core.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PorcionDisponibleDTO {
    private Long productoId;
    private String nombreProducto; 
    private BigDecimal porcionesDisponibles;
    private String nivelAdvertencia;
    private String estadoDisponibilidad; 

    public void setPorcionesDisponibles(Integer porciones) {
        this.porcionesDisponibles = new BigDecimal(porciones);
    }
    
    public void setPorcionesDisponibles(BigDecimal porcionesDisponibles) {
        this.porcionesDisponibles = porcionesDisponibles;
    }
}