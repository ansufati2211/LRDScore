package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MAGIA MULTI-TENANT: Hibernate filtrará automáticamente por este campo.
    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    // Eliminamos @Temporal y cambiamos a LocalDateTime (Resuelve java:S1874)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}