package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PagoRequestDTO {
    private Long sesionCajaId;
    private List<PagoItemDTO> pagos; // Permite pagar 10.00 en Efectivo y 2.50 con Yape
}