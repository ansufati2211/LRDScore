package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
public class Producto extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_base", length = 100)
    private String nombreBase;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

    @Column(name = "es_preparado")
    private Boolean esPreparado = true;

    @Column(name = "variante", length = 20)
    private String variante = "UNICO";

    @Column(name = "tiempo_preparacion_minutos")
    private Integer tiempoPreparacionMinutos = 5;

    @Column(name = "tags_busqueda", columnDefinition = "TEXT")
    private String tagsBusqueda;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "costo_referencial", precision = 12, scale = 4)
    private BigDecimal costoReferencial;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;
}