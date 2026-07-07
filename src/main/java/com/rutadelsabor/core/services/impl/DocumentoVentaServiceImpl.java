package com.rutadelsabor.core.services.impl;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.AnularDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.request.EmitirDocumentoVentaRequestDTO;
import com.rutadelsabor.core.dto.response.DocumentoVentaResponseDTO;
import com.rutadelsabor.core.exceptions.ModuloNoHabilitadoException;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.DocumentoCobro;
import com.rutadelsabor.core.models.entities.DocumentoVenta;
import com.rutadelsabor.core.models.entities.Pedido;
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
        if (dto.getTipo() == null || dto.getTipo().isBlank()) {
            throw new ReglaNegocioException("El tipo de documento es obligatorio.");
        }

        TipoDocumentoVenta tipo;
        try {
            tipo = TipoDocumentoVenta.valueOf(dto.getTipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de documento inválido: " + dto.getTipo() + ". Use NOTA_VENTA, BOLETA o FACTURA.");
        }

        // E7-1: BOLETA y FACTURA solo con módulo FACTURACION habilitado
        if (tipo != TipoDocumentoVenta.NOTA_VENTA) {
            Long empresaId = TenantContext.getCurrentTenant();
            List<String> modulos = suscripcionService.obtenerModulosHabilitados(empresaId);
            if (!modulos.contains(Modulo.FACTURACION.name())) {
                throw new ModuloNoHabilitadoException(Modulo.FACTURACION);
            }
        }

        // E7-2: FACTURA exige RUC válido (11 dígitos)
        if (tipo == TipoDocumentoVenta.FACTURA) {
            validarRuc(dto.getNumeroDocumentoReceptor());
        }

        if (dto.getPedidoId() == null && dto.getDocumentoCobroId() == null) {
            throw new ReglaNegocioException("Debe indicar pedidoId o documentoCobroId.");
        }

        BigDecimal totalOrigen = resolverTotal(dto);
        BigDecimal subtotal;
        BigDecimal igv;
        BigDecimal total = totalOrigen;

        if (tipo == TipoDocumentoVenta.NOTA_VENTA) {
            // Comprobante interno: sin desglose de IGV
            subtotal = totalOrigen;
            igv = BigDecimal.ZERO;
        } else {
            // BOLETA/FACTURA — precio de venta incluye IGV peruano 18 %
            subtotal = totalOrigen.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
            igv = totalOrigen.subtract(subtotal);
        }

        // R7-1: correlativo atómico, sin huecos, por (empresa_id, tipo)
        String serie = serieParaTipo(tipo);
        Long empresaId = TenantContext.getCurrentTenant();
        int correlativo = obtenerSiguienteCorrelativo(empresaId, tipo.name(), serie);

        DocumentoVenta doc = new DocumentoVenta();
        doc.setTipo(tipo);
        doc.setSerie(serie);
        doc.setCorrelativo(correlativo);
        doc.setSubtotal(subtotal);
        doc.setIgv(igv);
        doc.setTotal(total);
        doc.setEstadoEmision(EstadoEmision.EMITIDO);
        doc.setFechaEmision(LocalDateTime.now(ZONA_HORARIA));
        doc.setTipoDocumentoReceptor(dto.getTipoDocumentoReceptor());
        doc.setNumeroDocumentoReceptor(dto.getNumeroDocumentoReceptor());
        doc.setRazonSocialReceptor(dto.getRazonSocialReceptor());

        if (dto.getPedidoId() != null) {
            Pedido pedido = pedidoRepository.findById(dto.getPedidoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + dto.getPedidoId() + NO_ENCONTRADO));
            doc.setPedido(pedido);
        }

        if (dto.getDocumentoCobroId() != null) {
            DocumentoCobro docCobro = documentoCobroRepository.findById(dto.getDocumentoCobroId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoCobro " + dto.getDocumentoCobroId() + NO_ENCONTRADO));
            doc.setDocumentoCobro(docCobro);
        }

        return mapToDTO(documentoVentaRepository.save(doc));
    }

    @Override
    @Transactional
    public DocumentoVentaResponseDTO anular(Long documentoId, AnularDocumentoVentaRequestDTO dto) {
        DocumentoVenta doc = documentoVentaRepository.findById(documentoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoVenta " + documentoId + NO_ENCONTRADO));

        if (doc.getEstadoEmision() == EstadoEmision.ANULADO) {
            throw new ReglaNegocioException("El comprobante ya está anulado.");
        }

        if (dto.getMotivo() == null || dto.getMotivo().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio.");
        }

        // E7-3: se registra como estado, jamás se borra el comprobante
        doc.setEstadoEmision(EstadoEmision.ANULADO);
        doc.setMotivoAnulacion(dto.getMotivo());

        return mapToDTO(documentoVentaRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoVentaResponseDTO obtenerPorId(Long documentoId) {
        return mapToDTO(documentoVentaRepository.findById(documentoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoVenta " + documentoId + NO_ENCONTRADO)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoVentaResponseDTO> listarPorPedido(Long pedidoId) {
        return documentoVentaRepository.findByPedidoId(pedidoId).stream()
                .map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoVentaResponseDTO> listarPorDocumentoCobro(Long documentoCobroId) {
        return documentoVentaRepository.findByDocumentoCobroId(documentoCobroId).stream()
                .map(this::mapToDTO).toList();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    // E7-2: RUC peruano = exactamente 11 dígitos numéricos
    private void validarRuc(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            throw new ReglaNegocioException(
                    "La FACTURA requiere un número de RUC válido (11 dígitos numéricos). Valor recibido: " + ruc);
        }
    }

    // Serie por tipo — una única serie por tenant en esta fase (multi-serie es SUNAT fase 2)
    private String serieParaTipo(TipoDocumentoVenta tipo) {
        return switch (tipo) {
            case NOTA_VENTA -> "NV01";
            case BOLETA     -> "B001";
            case FACTURA    -> "F001";
        };
    }

    // Resuelve el total origen desde el pedido o el documento de cobro
    private BigDecimal resolverTotal(EmitirDocumentoVentaRequestDTO dto) {
        if (dto.getDocumentoCobroId() != null) {
            DocumentoCobro docCobro = documentoCobroRepository.findById(dto.getDocumentoCobroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("DocumentoCobro " + dto.getDocumentoCobroId() + NO_ENCONTRADO));
            return docCobro.getTotal();
        }
        Pedido pedido = pedidoRepository.findById(dto.getPedidoId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + dto.getPedidoId() + NO_ENCONTRADO));
        return pedido.getTotal();
    }

    /**
     * R7-1: incremento atómico del correlativo usando INSERT ON CONFLICT DO UPDATE.
     * Una sola sentencia SQL garantiza ausencia de huecos en entornos concurrentes y
     * multi-nodo sin necesidad de bloqueos a nivel de aplicación.
     */
    @SuppressWarnings("unchecked")
    private int obtenerSiguienteCorrelativo(Long empresaId, String tipo, String serie) {
        List<Number> resultado = entityManager.createNativeQuery(
                "INSERT INTO series_correlativo (empresa_id, tipo, serie, ultimo_correlativo, updated_at) " +
                "VALUES (:empresaId, :tipo, :serie, 1, NOW()) " +
                "ON CONFLICT (empresa_id, tipo) DO UPDATE " +
                "  SET ultimo_correlativo = series_correlativo.ultimo_correlativo + 1, " +
                "      updated_at = NOW() " +
                "RETURNING ultimo_correlativo"
        )
        .setParameter("empresaId", empresaId)
        .setParameter("tipo", tipo)
        .setParameter("serie", serie)
        .getResultList();

        if (resultado.isEmpty()) {
            throw new IllegalStateException("No se pudo generar el correlativo para tipo=" + tipo);
        }
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
        dto.setMotivoAnulacion(doc.getMotivoAnulacion());
        dto.setPedidoId(doc.getPedido() != null ? doc.getPedido().getId() : null);
        dto.setDocumentoCobroId(doc.getDocumentoCobro() != null ? doc.getDocumentoCobro().getId() : null);
        dto.setDocumentoReferenciaId(doc.getDocumentoReferencia() != null ? doc.getDocumentoReferencia().getId() : null);
        return dto;
    }
}
