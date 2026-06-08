package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auditoria_logs")
public class AuditoriaLog extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String modulo; // Ej: CAJA, KDS, ADMIN

    @Column(nullable = false, length = 255)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String detalle;
}