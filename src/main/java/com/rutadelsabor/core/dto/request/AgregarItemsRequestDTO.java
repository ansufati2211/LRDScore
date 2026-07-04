package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AgregarItemsRequestDTO {

    private List<ItemDTO> items;

    @Getter
    @Setter
    public static class ItemDTO {
        private Long productoId;
        private Integer cantidad;
        private String notasPreparacion;
    }
}
