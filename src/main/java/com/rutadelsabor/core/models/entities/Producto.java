package com.rutadelsabor.core.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <-- 1. IMPORTANTE AÑADIR ESTO
import com.rutadelsabor.core.models.enums.EstadoDisponibilidad;
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
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- 2. ESTA ES LA CURA PARA EL ERROR 500
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_disponibilidad", nullable = false, length = 20)
    private EstadoDisponibilidad estadoDisponibilidad = EstadoDisponibilidad.DISPONIBLE;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;

    public Long getCategoriaId() {
        return this.categoria != null ? this.categoria.getId() : null;
    }
}