package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categorias")
public class Categoria extends BaseTenantEntity {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;
}