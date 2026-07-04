package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DocumentoVentaResponseDTO {

    private Long id;
    private String tipo;
    private String serie;
    private Integer correlativo;
    // Formato SUNAT: "NV01-0000001" / "B001-0000001" / "F001-0000001"
    private String numeroDocumento;

    private String tipoDocumentoReceptor;
    private String numeroDocumentoReceptor;
    private String razonSocialReceptor;

    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    private String estadoEmision;
    private String fechaEmision;
    private String motivoAnulacion;

    private Long pedidoId;
    private Long documentoCobroId;
    private Long documentoReferenciaId;
}
