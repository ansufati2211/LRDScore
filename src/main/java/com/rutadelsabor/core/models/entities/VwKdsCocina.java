package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Date;

@Entity
@Table(name = "vw_kds_cocina")
@Getter
public class VwKdsCocina {

    @Id
    @Column(name = "detalle_id")
    private Long detalleId; // Usamos el ID del detalle como llave primaria de lectura

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "mesa")
    private String mesa;

    @Column(name = "producto")
    private String producto;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "estado_pedido")
    private String estadoPedido;

    @Column(name = "notas_preparacion")
    private String notasPreparacion;

    @Column(name = "fecha_pedido")
    private Date fechaPedido;

    @Column(name = "empresa_id")
    private Long empresaId;
}