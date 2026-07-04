package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnularDocumentoVentaRequestDTO {
    // E7-3: la anulación se registra como estado+motivo, jamás como borrado
    private String motivo;
}
