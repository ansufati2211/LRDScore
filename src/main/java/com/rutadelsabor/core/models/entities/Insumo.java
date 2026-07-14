package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "insumos")
@Getter
@Setter
public class Insumo extends BaseTenantEntity {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;
}