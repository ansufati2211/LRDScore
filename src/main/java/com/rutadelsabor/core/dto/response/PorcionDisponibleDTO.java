package com.rutadelsabor.core.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PorcionDisponibleDTO {
    private Long productoId;
    private String nombreProducto; // FIX: añadido
    private BigDecimal porcionesDisponibles;
    private String nivelAdvertencia; // FIX: añadido

    // FIX: Setter sobrecargado para aceptar Integer y mantener compatibilidad con el servicio
    public void setPorcionesDisponibles(Integer porciones) {
        this.porcionesDisponibles = new BigDecimal(porciones);
    }
    
    // Método original para soportar inyección de BigDecimal si fuera necesario
    public void setPorcionesDisponibles(BigDecimal porcionesDisponibles) {
        this.porcionesDisponibles = porcionesDisponibles;
    }
}