package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "clientes")
public class Cliente extends BaseTenantEntity {

    @Column(name = "nombre_razon_social", nullable = false, length = 150)
    private String nombreRazonSocial;

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(length = 100)
    private String correo;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;
}