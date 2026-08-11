package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmitirDocumentoVentaRequestDTO {
    private String tipo;
    private Long pedidoId;
    private Long documentoCobroId;
    private String tipoDocumentoReceptor;  
    private String numeroDocumentoReceptor;
    private String razonSocialReceptor;
    private Long sedeId;
}
