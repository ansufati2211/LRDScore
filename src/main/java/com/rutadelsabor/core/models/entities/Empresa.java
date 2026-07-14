package com.rutadelsabor.core.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_comercial", nullable = false, length = 100)
    private String nombreComercial;

    @Column(length = 11)
    private String ruc;

    @Column(length = 255)
    private String direccion;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;

    // Referencia a la suscripción vigente del tenant (FK opcional en empresas.suscripcion_vigente_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "suscripcion_vigente_id")
    @JsonIgnoreProperties("empresa")
    private Suscripcion suscripcionVigente;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

@PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
