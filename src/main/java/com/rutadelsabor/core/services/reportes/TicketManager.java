package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.PedidoDetalle;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TicketManager {

    // SOLUCIÓN java:S1192: Uso de constantes para evitar la duplicación de literales
    private static final String DOUBLE_LINE = "========================================\n";
    private static final String SINGLE_LINE = "----------------------------------------\n";

    public String generarTicketTermico(Pedido pedido) {
        StringBuilder ticket = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ticket.append(DOUBLE_LINE);
        ticket.append("          LA RUTA DEL SABOR           \n");
        ticket.append(DOUBLE_LINE);
        
        // SOLUCIÓN java:S2154: Conversión explícita a String para evitar conflictos de Casteo entre Integer y Long
        String identificadorOrden = pedido.getNumeroOrden() != null 
                ? String.valueOf(pedido.getNumeroOrden()) 
                : String.valueOf(pedido.getId());
                
        ticket.append("Orden Nro: ").append(identificadorOrden).append("\n");
        ticket.append("Fecha: ").append(pedido.getCreatedAt().format(formatter)).append("\n");
        ticket.append("Atendido por: ").append(pedido.getMozo().getNombre()).append("\n");
        ticket.append("Tipo: ").append(pedido.getTipoConsumo()).append("\n");
        
        if (pedido.getIdentificadorMesaReferencia() != null) {
            ticket.append("Mesa: ").append(pedido.getIdentificadorMesaReferencia()).append("\n");
        }
        
        ticket.append(SINGLE_LINE);
        // SOLUCIÓN java:S3457: Usar %n para saltos de línea dinámicos en String.format
        ticket.append(String.format("%-2s %-25s %8s%n", "C.", "PRODUCTO", "SUBT"));
        ticket.append(SINGLE_LINE);

        for (PedidoDetalle detalle : pedido.getDetalles()) {
            String nombreProd = detalle.getProducto().getNombre();
            if (nombreProd.length() > 25) {
                nombreProd = nombreProd.substring(0, 22) + "...";
            }
            // SOLUCIÓN java:S3457: Usar %n
            ticket.append(String.format("%-2d %-25s S/%6.2f%n", 
                    detalle.getCantidad(), 
                    nombreProd, 
                    detalle.getSubtotal()));
        }

        ticket.append(SINGLE_LINE);
        // SOLUCIÓN java:S3457: Se retira el 3er parámetro redundante y se usa %n
        ticket.append(String.format("%-28s S/%6.2f%n", "TOTAL A PAGAR:", pedido.getTotal()));
        ticket.append(DOUBLE_LINE);
        ticket.append("       ¡Gracias por su preferencia!     \n");
        ticket.append(DOUBLE_LINE);

        return ticket.toString();
    }
}