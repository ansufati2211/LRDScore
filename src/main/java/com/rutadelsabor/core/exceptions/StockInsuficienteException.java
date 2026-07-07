package com.rutadelsabor.core.exceptions;

import com.rutadelsabor.core.dto.response.InsumoFaltanteDTO;

import java.util.List;

public class StockInsuficienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<InsumoFaltanteDTO> faltantes;

    public StockInsuficienteException(List<InsumoFaltanteDTO> faltantes) {
        super("Stock insuficiente para " + faltantes.size() + " insumo(s)");
        this.faltantes = faltantes;
    }

    public List<InsumoFaltanteDTO> getFaltantes() { return faltantes; }
}
