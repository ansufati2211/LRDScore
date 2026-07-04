package com.rutadelsabor.core.exceptions;

import com.rutadelsabor.core.dto.response.InsumoFaltanteDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class StockInsuficienteResponseDTO extends ErrorResponseDTO {

    private List<InsumoFaltanteDTO> faltantes;

    public StockInsuficienteResponseDTO(LocalDateTime timestamp, int status, String error,
                                         String message, String path, String codigo,
                                         List<InsumoFaltanteDTO> faltantes) {
        super(timestamp, status, error, message, path, codigo);
        this.faltantes = faltantes;
    }
}
