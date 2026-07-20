package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoEmision;
import com.rutadelsabor.core.models.enums.TipoDocumentoVenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "documentos_venta")
public class DocumentoVenta extends BaseSedeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_cobro_id")
    private DocumentoCobro documentoCobro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoDocumentoVenta tipo;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    @Column(name = "tipo_documento_receptor", length = 20)
    private String tipoDocumentoReceptor;

    @Column(name = "numero_documento_receptor", length = 20)
    private String numeroDocumentoReceptor;

    @Column(name = "razon_social_receptor", length = 200)
    private String razonSocialReceptor;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_emision", nullable = false, length = 20)
    private EstadoEmision estadoEmision = EstadoEmision.EMITIDO;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_referencia_id")
    private DocumentoVenta documentoReferencia;
}