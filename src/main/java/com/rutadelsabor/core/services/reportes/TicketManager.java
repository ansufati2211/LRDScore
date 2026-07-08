package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.entities.PedidoDetalle;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TicketManager {

    private static final String DOUBLE_LINE = "========================================\n";
    private static final String SINGLE_LINE = "----------------------------------------\n";

    private final SedeRepository sedeRepository;

    public TicketManager(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public String generarTicketTermico(Pedido pedido) {
        StringBuilder ticket = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Obtenemos los datos reales de la sede donde se emitió el pedido
        Sede sede = sedeRepository.findById(pedido.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada para el ticket"));

        ticket.append(DOUBLE_LINE);
        ticket.append("          LA RUTA DEL SABOR           \n");
        ticket.append("          ").append(sede.getNombre()).append("          \n");
        ticket.append("Dir: ").append(sede.getDireccion() != null ? sede.getDireccion() : "S/D").append("\n");
        ticket.append("Establecimiento SUNAT: ").append(sede.getCodigoEstablecimiento() != null ? sede.getCodigoEstablecimiento() : "S/D").append("\n");
        ticket.append(DOUBLE_LINE);
        
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
        ticket.append(String.format("%-2s %-25s %8s%n", "C.", "PRODUCTO", "SUBT"));
        ticket.append(SINGLE_LINE);

        for (PedidoDetalle detalle : pedido.getDetalles()) {
            String nombreProd = detalle.getProducto().getNombre();
            if (nombreProd.length() > 25) {
                nombreProd = nombreProd.substring(0, 22) + "...";
            }
            ticket.append(String.format("%-2d %-25s S/%6.2f%n", 
                    detalle.getCantidad(), 
                    nombreProd, 
                    detalle.getSubtotal()));
        }

        ticket.append(SINGLE_LINE);
        ticket.append(String.format("%-28s S/%6.2f%n", "TOTAL A PAGAR:", pedido.getTotal()));
        ticket.append(DOUBLE_LINE);
        ticket.append("       ¡Gracias por su preferencia!     \n");
        ticket.append(DOUBLE_LINE);

        return ticket.toString();
    }
}