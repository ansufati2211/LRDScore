package com.rutadelsabor.core.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
// PROTECCIÓN DE SEGURIDAD: Evita fugas del hash si la entidad se serializa
@JsonIgnoreProperties({"passwordHash"}) 
public class Usuario extends BaseTenantEntity {

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;
}