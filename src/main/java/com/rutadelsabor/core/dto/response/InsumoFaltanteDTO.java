package com.rutadelsabor.core.dto.response;

import java.math.BigDecimal;

public record InsumoFaltanteDTO(String nombreInsumo, BigDecimal disponible, BigDecimal requerido) {}
