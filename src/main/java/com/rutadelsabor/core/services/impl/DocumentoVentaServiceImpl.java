package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.AnularDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.request.EmitirDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoVentaResponseDTO;
import com.rutadelsabor.core.exceptions.ModuloNoHabilitadoException;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.DocumentoVenta;
import com.rutadelsabor.core.models.enums.EstadoEmision;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.models.enums.TipoDocumentoVenta;
import com.rutadelsabor.core.repositories.DocumentoCobroRepository;
import com.rutadelsabor.core.repositories.DocumentoVentaRepository;
import com.rutadelsabor.core.repositories.PedidoRepository;
import com.rutadelsabor.core.services.interfaces.IDocumentoVentaService;
import com.rutadelsabor.core.services.interfaces.ISuscripcionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DocumentoVentaServiceImpl implements IDocumentoVentaService {

    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final String NO_ENCONTRADO = " no encontrado";

    private final DocumentoVentaRepository documentoVentaRepository;
    private final PedidoRepository pedidoRepository;
    private final DocumentoCobroRepository documentoCobroRepository;
    private final ISuscripcionService suscripcionService;

    @PersistenceContext
    private EntityManager entityManager;

    public DocumentoVentaServiceImpl(DocumentoVentaRepository documentoVentaRepository,
                                     PedidoRepository pedidoRepository,
                                     DocumentoCobroRepository documentoCobroRepository,
                                     ISuscripcionService suscripcionService) {
        this.documentoVentaRepository = documentoVentaRepository;
        this.pedidoRepository = pedidoRepository;
        this.documentoCobroRepository = documentoCobroRepository;
        this.suscripcionService = suscripcionService;
    }

    @Override
    @Transactional
    public DocumentoVentaResponseDTO emitir(EmitirDocumentoVentaRequestDTO dto) {
        if (dto.getTipo() == null || dto.getTipo().isBlank()) throw new ReglaNegocioException("Tipo de documento obligatorio.");
        
        TipoDocumentoVenta tipo;
        try { tipo = TipoDocumentoVenta.valueOf(dto.getTipo().toUpperCase()); } 
        catch (IllegalArgumentException e) { throw new ReglaNegocioException("Tipo inválido. Use NOTA_VENTA, BOLETA o FACTURA."); }

        if (tipo != TipoDocumentoVenta.NOTA_VENTA) {
            List<String> modulos = suscripcionService.obtenerModulosHabilitados(TenantContext.getCurrentTenant());
            if (!modulos.contains(Modulo.FACTURACION.name())) throw new ModuloNoHabilitadoException(Modulo.FACTURACION);
        }

        if (tipo == TipoDocumentoVenta.FACTURA) validarRuc(dto.getNumeroDocumentoReceptor());
        if (dto.getPedidoId() == null && dto.getDocumentoCobroId() == null) throw new ReglaNegocioException("Indique pedidoId o documentoCobroId.");

        BigDecimal totalOrigen = resolverTotal(dto);
        BigDecimal subtotal = (tipo == TipoDocumentoVenta.NOTA_VENTA) ? totalOrigen : totalOrigen.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal igv = totalOrigen.subtract(subtotal);

        Long empresaId = TenantContext.getCurrentTenant();
        Long sedeEfectiva = TenantContext.resolverSedeEfectiva(dto.getSedeId());
        String serie = serieParaTipo(tipo);
        int correlativo = obtenerSiguienteCorrelativo(empresaId, sedeEfectiva, tipo.name(), serie);

        DocumentoVenta doc = new DocumentoVenta();
        doc.setTipo(tipo);
        doc.setSerie(serie);
        doc.setCorrelativo(correlativo);
        doc.setSedeId(sedeEfectiva);
        doc.setSubtotal(subtotal);
        doc.setIgv(igv);
        doc.setTotal(totalOrigen);
        doc.setEstadoEmision(EstadoEmision.EMITIDO);
        doc.setFechaEmision(LocalDateTime.now(ZONA_HORARIA));
        doc.setTipoDocumentoReceptor(dto.getTipoDocumentoReceptor());
        doc.setNumeroDocumentoReceptor(dto.getNumeroDocumentoReceptor());
        doc.setRazonSocialReceptor(dto.getRazonSocialReceptor());

        if (dto.getPedidoId() != null) {
            doc.setPedido(pedidoRepository.findById(dto.getPedidoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Pedido" + NO_ENCONTRADO)));
        }

        if (dto.getDocumentoCobroId() != null) {
            doc.setDocumentoCobro(documentoCobroRepository.findById(dto.getDocumentoCobroId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoCobro" + NO_ENCONTRADO)));
        }

        return mapToDTO(documentoVentaRepository.save(doc));
    }

    @Override
    @Transactional
    public DocumentoVentaResponseDTO anular(Long documentoId, AnularDocumentoVentaRequestDTO dto) {
        DocumentoVenta doc = documentoVentaRepository.findById(documentoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoVenta " + documentoId + NO_ENCONTRADO));
        if (doc.getEstadoEmision() == EstadoEmision.ANULADO) throw new ReglaNegocioException("El comprobante ya está anulado.");
        if (dto.getMotivo() == null || dto.getMotivo().isBlank()) throw new ReglaNegocioException("Motivo obligatorio.");
        doc.setEstadoEmision(EstadoEmision.ANULADO);
        doc.setMotivoAnulacion(dto.getMotivo());
        return mapToDTO(documentoVentaRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoVentaResponseDTO obtenerPorId(Long id) {
        return mapToDTO(documentoVentaRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoVentaResponseDTO> listarPorPedido(Long id) {
        return documentoVentaRepository.findByPedidoId(id).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoVentaResponseDTO> listarPorDocumentoCobro(Long id) {
        return documentoVentaRepository.findByDocumentoCobroId(id).stream().map(this::mapToDTO).toList();
    }

    private void validarRuc(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) throw new ReglaNegocioException("RUC inválido.");
    }

    private String serieParaTipo(TipoDocumentoVenta tipo) {
        return switch (tipo) { case NOTA_VENTA -> "NV01"; case BOLETA -> "B001"; case FACTURA -> "F001"; };
    }

    private BigDecimal resolverTotal(EmitirDocumentoVentaRequestDTO dto) {
        if (dto.getDocumentoCobroId() != null) return documentoCobroRepository.findById(dto.getDocumentoCobroId()).orElseThrow().getTotal();
        return pedidoRepository.findById(dto.getPedidoId()).orElseThrow().getTotal();
    }

    @SuppressWarnings("unchecked")
    private int obtenerSiguienteCorrelativo(Long empresaId, Long sedeId, String tipo, String serie) {
        List<Number> resultado = entityManager.createNativeQuery(
                "INSERT INTO series_correlativo (empresa_id, sede_id, tipo, serie, ultimo_correlativo, updated_at) " +
                "VALUES (:empresaId, :sedeId, :tipo, :serie, 1, NOW()) " +
                "ON CONFLICT (sede_id, tipo) DO UPDATE " +
                "  SET ultimo_correlativo = series_correlativo.ultimo_correlativo + 1, updated_at = NOW() " +
                "RETURNING ultimo_correlativo"
        )
        .setParameter("empresaId", empresaId)
        .setParameter("sedeId", sedeId)
        .setParameter("tipo", tipo)
        .setParameter("serie", serie)
        .getResultList();
        if (resultado.isEmpty()) throw new IllegalStateException("Error al generar correlativo.");
        return resultado.get(0).intValue();
    }

    private DocumentoVentaResponseDTO mapToDTO(DocumentoVenta doc) {
        DocumentoVentaResponseDTO dto = new DocumentoVentaResponseDTO();
        dto.setId(doc.getId());
        dto.setTipo(doc.getTipo().name());
        dto.setSerie(doc.getSerie());
        dto.setCorrelativo(doc.getCorrelativo());
        dto.setNumeroDocumento(doc.getSerie() + "-" + String.format("%07d", doc.getCorrelativo()));
        dto.setTipoDocumentoReceptor(doc.getTipoDocumentoReceptor());
        dto.setNumeroDocumentoReceptor(doc.getNumeroDocumentoReceptor());
        dto.setRazonSocialReceptor(doc.getRazonSocialReceptor());
        dto.setSubtotal(doc.getSubtotal());
        dto.setIgv(doc.getIgv());
        dto.setTotal(doc.getTotal());
        dto.setEstadoEmision(doc.getEstadoEmision().name());
        dto.setFechaEmision(doc.getFechaEmision() != null ? doc.getFechaEmision().toString() : null);
        return dto;
    }
}