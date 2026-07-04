package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmitirDocumentoVentaRequestDTO {

    // "NOTA_VENTA" | "BOLETA" | "FACTURA"
    private String tipo;

    // Exactamente uno de los dos debe estar presente
    private Long pedidoId;
    private Long documentoCobroId;

    // Receptor — R7-4: opcional en NOTA_VENTA; obligatorio en BOLETA/FACTURA
    // FACTURA exige tipoDocumentoReceptor=RUC + numeroDocumentoReceptor de 11 dígitos (E7-2)
    private String tipoDocumentoReceptor;   // "DNI" | "RUC" | "CE" | "CONSUMIDOR_FINAL"
    private String numeroDocumentoReceptor;
    private String razonSocialReceptor;
}
