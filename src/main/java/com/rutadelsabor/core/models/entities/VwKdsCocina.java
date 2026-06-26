package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "vw_kds_cocina")
@Getter
@Setter
@Immutable // Protege la vista de modificaciones accidentales
public class VwKdsCocina {

    @Id
    @Column(name = "detalle_id")
    private Long detalleId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "numero_orden")
    private Integer numeroOrden;

    @Column(name = "tipo_consumo")
    private String tipoConsumo;

    @Column(name = "mesa")
    private String mesa;

    @Column(name = "estado_pedido")
    private String estadoPedido;

    @Column(name = "notas_generales")
    private String notasGenerales;

    @Column(name = "hora_ingreso")
    private LocalDateTime horaIngreso;

    @Column(name = "minutos_transcurridos")
    private Long minutosTranscurridos;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "notas_preparacion")
    private String notasPreparacion;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "producto")
    private String producto;

    @Column(name = "tiempo_preparacion_minutos")
    private Integer tiempoPreparacionMinutos;
}