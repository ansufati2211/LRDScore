package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseSedeEntity extends BaseTenantEntity {
    
    @Column(name = "sede_id", nullable = false)
    private Long sedeId;
}