package com.rutadelsabor.core.models.entities;

import lombok.Data;
import java.io.Serializable;

@Data
public class UsuarioSedeId implements Serializable {
    private Long usuarioId;
    private Long sedeId;
}