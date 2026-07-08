package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sedes")
public class Sede extends BaseTenantEntity {

    @Column(name = "codigo_establecimiento", length = 10)
    private String codigoEstablecimiento;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(name = "estado_registro", nullable = false)
    private Boolean estadoRegistro = true;
}