package com.rutadelsabor.core.models.entities;

import lombok.Data;
import java.io.Serializable;

@Data
public class InsumoSedeId implements Serializable {
    private Long insumoId;
    private Long sedeId;
}