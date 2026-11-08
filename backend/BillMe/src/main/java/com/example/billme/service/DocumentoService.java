package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.calculo.CalculoDocumento.LineaCalculada;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.LineaResponse;
import com.example.billme.dto.PaginaResponse;
import com.example.billme.dto.RevisionResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.MetodoPrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.exportacion.ArchivoExcel;
import com.example.billme.exportacion.DocumentoExcel;
import com.example.billme.exportacion.NombreArchivo;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.DocumentoRevision;
import com.example.billme.model.Producto;
import com.example.billme.model.Proyecto;
import com.example.billme.model.Usuario;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.DocumentoRevisionRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.ProyectoRepository;
import com.example.billme.repository.UsuarioRepository;
import com.example.billme.util.Fechas;
import com.example.billme.util.NormalizadorTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cotizaciones y facturas: series de folio, cálculo de totales, conversión y anulación. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ClienteRepository clienteRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrecioListaService precioListaService;
    private final DocumentoRevisionRepository revisionRepository;
    private final ConfiguracionService configuracionService;
    private final PrecioPorPesoService precioPorPesoService;
    private final JsonMapper jsonMapper;
    private final DocumentoExcel documentoExcel;

    public PaginaResponse<DocumentoResponse> filtrar(TipoDocumento tipo, EstadoDocumento estado, UUID clienteId, UUID proyectoId,
                                                    UUID productoId, LocalDate desde, LocalDate hasta, String q, Pageable pageable) {
        String qNormalizada = NormalizadorTexto.sinAcentos(q);
        String contiene = "%" + qNormalizada + "%";
        Page<Documento> pagina = documentoRepository.filtrar(tipo, estado, clienteId, proyectoId, productoId, desde, hasta, qNormalizada, contiene, pageable);
        List<DocumentoResponse> elementos = pagina.getContent().stream()
                .map(d -> toResponse(d, null))
                .toList();
        return new PaginaResponse<>(elementos, pagina.getNumber(), pagina.getTotalPages(), pagina.getTotalElements());
    }

    public List<DocumentoResponse> filtrar(TipoDocumento tipo, EstadoDocumento estado, UUID clienteId, UUID proyectoId,
                                           UUID productoId, LocalDate desde, LocalDate hasta) {
        return filtrar(tipo, estado, clienteId, proyectoId, productoId, desde, hasta, "", Pageable.unpaged()).contenido();
    }

    public DocumentoResponse obtener(UUID id) {
        Documento documento = buscar(id);
        return toResponse(documento, documentoRepository.findFirstByCotizacionOrigenIdAndEstadoNot(id, EstadoDocumento.ANULADA).orElse(null));
    }

    public ArchivoExcel exportarExcel(UUID id, String encabezado) {
        Documento doc = buscar(id);
        String encabezadoFinal = (encabezado != null && !encabezado.isBlank())
                ? encabezado.trim()
                : encabezadoPorDefecto(doc);
        byte[] contenido = documentoExcel.generar(doc, encabezadoFinal);
        String clienteNombre = doc.getCliente() != null && doc.getCliente().getNombre() != null
                ? doc.getCliente().getNombre()
                : "";
        String nombre = NombreArchivo.de(doc.getFolio(), clienteNombre, doc.getFecha(), "xlsx");
        return new ArchivoExcel(nombre, contenido);
    }

    public String encabezadoPorDefecto(Documento doc) {
        String clienteNombre = doc.getCliente() != null && doc.getCliente().getNombre() != null
                ? doc.getCliente().getNombre()
                : "";
        StringBuilder sb = new StringBuilder(clienteNombre);
        if (doc.getProyecto() != null && doc.getProyecto().getNombre() != null && !doc.getProyecto().getNombre().isBlank()) {
            sb.append(" / ").append(doc.getProyecto().getNombre().trim());
        }
        if (doc.getTipo() == TipoDocumento.COTIZACION) {
            sb.append(" / Cotización");
        }
        return sb.toString();
    }

    @Transactional
    public DocumentoResponse crear(DocumentoRequest request) {
        if (request.tipo() == null || request.tipo() == TipoDocumento.HISTORICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica si es cotización o factura");
        }
        Documento documento = Documento.builder()
                .tipo(request.tipo())
                .estado(request.tipo() == TipoDocumento.COTIZACION ? EstadoDocumento.PENDIENTE : EstadoDocumento.ACTIVA)
                .folio(siguienteFolio(request.tipo()))
                .tipoCambio(configuracionService.tipoCambio().setScale(6, RoundingMode.HALF_UP))
                .usuario(usuarioActual())
                .build();
        aplicar(documento, request);
        return toResponse(documentoRepository.saveAndFlush(documento), null);
    }

    String copia(Documento documento) {
        return jsonMapper.writeValueAsString(toResponse(documento, null));
    }

    void registrarRevision(Documento documento, String copiaAnterior, String motivo) {
        revisionRepository.save(DocumentoRevision.builder()
                .documento(documento)
                .numero(revisionRepository.countByDocumentoId(documento.getId()) + 1)
                .snapshot(copiaAnterior)
                .motivo(motivo)
                .usuario(usuarioActual())
                .build());
    }

    @Transactional
    public DocumentoResponse actualizar(UUID id, DocumentoRequest request) {
        Documento documento = buscar(id);
        if (documento.getEstado() == EstadoDocumento.ANULADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reactiva la factura para poder editarla");
        }
        if (documento.getEstado() == EstadoDocumento.ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta cotización ya se convirtió en factura: edita la factura");
        }
        if (documento.getEstado() == EstadoDocumento.HISTORICA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este registro del histórico no se edita: conviértelo primero en factura activa");
        }
        String copiaAnterior = copia(documento);
        String antes = huella(documento);
        aplicar(documento, request);
        // Guardar sin cambios no deja una versión nueva
        if (!huella(documento).equals(antes)) {
            registrarRevision(documento, copiaAnterior, "Edición");
        }
        return toResponse(documentoRepository.saveAndFlush(documento), null);
    }

    /** Versiones anteriores de un documento, la más reciente primero. */
    public List<RevisionResponse> revisiones(UUID id) {
        buscar(id);
        return revisionRepository.findByDocumentoIdOrderByNumeroDesc(id).stream()
                .map(r -> new RevisionResponse(
                        r.getNumero(),
                        r.getCreatedAt(),
                        r.getUsuario() != null ? r.getUsuario().getNombre() : null,
                        r.getMotivo(),
                        jsonMapper.readValue(r.getSnapshot(), DocumentoResponse.class)))
                .toList();
    }

    /** Crea la factura de una cotización pendiente (con las mismas líneas) y marca la cotización como aceptada. */
    @Transactional
    public DocumentoResponse convertirAFactura(UUID id, LocalDate fecha) {
        Documento cotizacion = buscar(id);
        if (cotizacion.getTipo() != TipoDocumento.COTIZACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo las cotizaciones se convierten en factura");
        }
        if (cotizacion.getEstado() != EstadoDocumento.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, cotizacion.getEstado() == EstadoDocumento.ACEPTADA
                    ? "Esta cotización ya se convirtió en factura"
                    : "Esta cotización está rechazada: reábrela para convertirla en factura");
        }
        String copiaCotizacion = copia(cotizacion);
        Documento factura = Documento.builder()
                .tipo(TipoDocumento.FACTURA)
                .estado(EstadoDocumento.ACTIVA)
                .folio(siguienteFolio(TipoDocumento.FACTURA))
                .fecha(fecha != null ? fecha : Fechas.hoy())
                .cliente(cotizacion.getCliente())
                .proyecto(cotizacion.getProyecto())
                .notas(cotizacion.getNotas())
                .mostrarEntrega(cotizacion.isMostrarEntrega())
                .mostrarTotalUsd(cotizacion.isMostrarTotalUsd())
                .tipoCambio(cotizacion.getTipoCambio())
                .total(cotizacion.getTotal())
                .totalKg(cotizacion.getTotalKg())
                .usuario(usuarioActual())
                .cotizacionOrigen(cotizacion)
                .build();
        for (DocumentoLinea origen : cotizacion.getLineas()) {
            factura.getLineas().add(DocumentoLinea.builder()
                    .documento(factura)
                    .orden(origen.getOrden())
                    .producto(origen.getProducto())
                    .descripcion(origen.getDescripcion())
                    .cantidad(origen.getCantidad())
                    .pesoUnitKg(origen.getPesoUnitKg())
                    .pesoTotalKg(origen.getPesoTotalKg())
                    .precioUnit(origen.getPrecioUnit())
                    .total(origen.getTotal())
                    .metodoPrecio(origen.getMetodoPrecio())
                    .parametros(origen.getParametros())
                    .fechaEntrega(origen.getFechaEntrega())
                    .lugarEntrega(origen.getLugarEntrega())
                    .costoUnitConIva(origen.getCostoUnitConIva())
                    .build());
        }
        cotizacion.setEstado(EstadoDocumento.ACEPTADA);
        Documento facturaGuardada = documentoRepository.saveAndFlush(factura);
        registrarRevision(cotizacion, copiaCotizacion, "Convertida en factura " + facturaGuardada.getFolio());
        return toResponse(facturaGuardada, null);
    }

    /** Una cotización se puede rechazar y reabrir. La aceptación solo ocurre al convertirla en factura. */
    @Transactional
    public DocumentoResponse cambiarEstadoCotizacion(UUID id, EstadoDocumento nuevo) {
        Documento documento = buscar(id);
        boolean rechazar = documento.getEstado() == EstadoDocumento.PENDIENTE && nuevo == EstadoDocumento.RECHAZADA;
        boolean reabrir = documento.getEstado() == EstadoDocumento.RECHAZADA && nuevo == EstadoDocumento.PENDIENTE;
        if (documento.getTipo() != TipoDocumento.COTIZACION || !(rechazar || reabrir)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese cambio de estado no es posible");
        }
        String copiaAnterior = copia(documento);
        documento.setEstado(nuevo);
        String motivo = rechazar ? "Cotización rechazada" : "Cotización reabierta";
        registrarRevision(documento, copiaAnterior, motivo);
        return toResponse(documento, null);
    }

    /** Una factura anulada deja de contar como ingreso y como precio dado, pero se conserva. */
    @Transactional
    public DocumentoResponse anular(UUID id) {
        Documento documento = buscar(id);
        if (documento.getTipo() != TipoDocumento.FACTURA || documento.getEstado() != EstadoDocumento.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede anular una factura activa");
        }
        String copiaAnterior = copia(documento);
        documento.setEstado(EstadoDocumento.ANULADA);
        registrarRevision(documento, copiaAnterior, "Factura anulada");
        Documento origen = documento.getCotizacionOrigen();
        if (origen != null && origen.getEstado() == EstadoDocumento.ACEPTADA) {
            String copiaOrigen = copia(origen);
            origen.setEstado(EstadoDocumento.PENDIENTE);
            registrarRevision(origen, copiaOrigen,
                    "Factura " + documento.getFolio() + " anulada: la cotización vuelve a pendiente");
        }
        return toResponse(documento, null);
    }

    /** Un registro del histórico pasa a ser una factura activa con folio propio. */
    @Transactional
    public DocumentoResponse convertirHistorico(UUID id) {
        Documento documento = buscar(id);
        if (documento.getTipo() != TipoDocumento.HISTORICO || documento.getEstado() != EstadoDocumento.HISTORICA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se convierte un registro del histórico");
        }
        String copiaAnterior = copia(documento);
        documento.setTipo(TipoDocumento.FACTURA);
        documento.setEstado(EstadoDocumento.ACTIVA);
        documento.setFolio(siguienteFolio(TipoDocumento.FACTURA));
        registrarRevision(documento, copiaAnterior, "Convertido en factura activa");
        return toResponse(documento, null);
    }

    /** Una factura anulada vuelve a estar activa, salvo que su cotización ya tenga otra factura. */
    @Transactional
    public DocumentoResponse reactivar(UUID id) {
        Documento documento = buscar(id);
        if (documento.getTipo() != TipoDocumento.FACTURA || documento.getEstado() != EstadoDocumento.ANULADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se reactiva una factura anulada");
        }
        Documento origen = documento.getCotizacionOrigen();
        if (origen != null) {
            if (documentoRepository.findFirstByCotizacionOrigenIdAndEstadoNot(origen.getId(), EstadoDocumento.ANULADA).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La cotización ya tiene otra factura: no se puede reactivar esta");
            }
            String copiaOrigen = copia(origen);
            origen.setEstado(EstadoDocumento.ACEPTADA);
            registrarRevision(origen, copiaOrigen, "Factura " + documento.getFolio() + " reactivada");
        }
        String copiaAnterior = copia(documento);
        documento.setEstado(EstadoDocumento.ACTIVA);
        registrarRevision(documento, copiaAnterior, "Factura reactivada");
        return toResponse(documento, null);
    }

    private Documento buscar(UUID id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    String siguienteFolio(TipoDocumento tipo) {
        return switch (tipo) {
            case COTIZACION -> "COT-%04d".formatted(documentoRepository.siguienteFolioCotizacion());
            case FACTURA -> "FAC-%04d".formatted(documentoRepository.siguienteFolioFactura());
            case HISTORICO -> "HIS-%04d".formatted(documentoRepository.siguienteFolioHistorico());
        };
    }

    /** El usuario en sesión; null si no corresponde a un usuario guardado. */
    Usuario usuarioActual() {
        Authentication sesion = SecurityContextHolder.getContext().getAuthentication();
        return sesion == null ? null : usuarioRepository.findByUsername(sesion.getName()).orElse(null);
    }

    private void aplicar(Documento documento, DocumentoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        Proyecto proyecto = null;
        if (request.proyectoId() != null) {
            proyecto = proyectoRepository.findById(request.proyectoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
            if (!proyecto.getCliente().getId().equals(cliente.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proyecto no pertenece a ese cliente");
            }
        }
        documento.setCliente(cliente);
        documento.setProyecto(proyecto);
        documento.setFecha(request.fecha());
        documento.setNotas(request.notas());
        documento.setMostrarEntrega(Boolean.TRUE.equals(request.mostrarEntrega()));
        documento.setMostrarTotalUsd(Boolean.TRUE.equals(request.mostrarTotalUsd()));
        aplicarLineas(documento, request.lineas());
    }

    private void aplicarLineas(Documento documento, List<LineaRequest> pedidas) {
        Map<UUID, DocumentoLinea> existentes = documento.getLineas().stream()
                .collect(Collectors.toMap(DocumentoLinea::getId, Function.identity()));
        Map<UUID, Producto> productos = productoRepository
                .findAllById(pedidas.stream().map(LineaRequest::productoId).distinct().toList()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        List<DocumentoLinea> lineas = new ArrayList<>();
        List<LineaCalculada> calculadas = new ArrayList<>();
        Map<UUID, BigDecimal> costos = new HashMap<>();
        for (LineaRequest pedida : pedidas) {
            Producto producto = productos.get(pedida.productoId());
            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
            }
            DocumentoLinea linea = pedida.id() != null ? existentes.get(pedida.id()) : new DocumentoLinea();
            if (linea == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La línea no pertenece a este documento");
            }
            boolean mismoProducto = linea.getProducto() != null && linea.getProducto().getId().equals(producto.getId());

            String descripcion = pedida.descripcion() == null ? "" : pedida.descripcion().trim();
            if (descripcion.isEmpty() && producto.isGenerico()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe la descripción de la línea");
            }
            BigDecimal cantidad = pedida.cantidad().setScale(3, RoundingMode.HALF_UP);
            BigDecimal pesoPedido = pedida.pesoUnitKg() != null ? pedida.pesoUnitKg() : producto.getPeso();
            BigDecimal precioPedido = pedida.precioUnit();
            if (pedida.metodoPrecio() == MetodoPrecio.POR_PESO) {
                boolean esNueva = linea.getId() == null;
                boolean cambioPeso = linea.getPesoUnitKg() == null || pesoPedido == null
                        || CalculoDocumento.r2(pesoPedido).compareTo(linea.getPesoUnitKg()) != 0;
                boolean cambioParametros = !Objects.equals(pedida.parametros(), linea.getParametros());
                if (esNueva || cambioPeso || cambioParametros) {
                    precioPedido = precioPorPesoService
                            .recalcular(pedida.parametros(), pesoPedido, documento.getTipoCambio())
                            .orElse(pedida.precioUnit());
                }
            }
            LineaCalculada calculada = CalculoDocumento.linea(cantidad, precioPedido, pesoPedido);
            if (calculada.total().compareTo(new BigDecimal("999999999999.99")) > 0
                    || (calculada.pesoTotalKg() != null && calculada.pesoTotalKg().compareTo(new BigDecimal("9999999999.99")) > 0)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La línea " + (lineas.size() + 1) + " tiene un total demasiado grande");
            }

            linea.setDocumento(documento);
            linea.setOrden(lineas.size());
            linea.setProducto(producto);
            linea.setDescripcion(descripcion.isEmpty() ? producto.getNombrePrincipal() : descripcion);
            linea.setCantidad(cantidad);
            linea.setPesoUnitKg(calculada.pesoUnitKg());
            linea.setPesoTotalKg(calculada.pesoTotalKg());
            linea.setPrecioUnit(calculada.precioUnit());
            linea.setTotal(calculada.total());
            linea.setMetodoPrecio(pedida.metodoPrecio() != null ? pedida.metodoPrecio() : MetodoPrecio.MANUAL);
            linea.setParametros(pedida.parametros());
            linea.setFechaEntrega(pedida.fechaEntrega());
            linea.setLugarEntrega(pedida.lugarEntrega() == null || pedida.lugarEntrega().isBlank()
                    ? null : pedida.lugarEntrega().trim());
            // Una línea que se conserva mantiene el costo con el que se vendió
            if (!mismoProducto) {
                linea.setCompra(null);
                linea.setCostoUnitConIva(costos.computeIfAbsent(producto.getId(),
                        p -> precioListaService.costoEstimadoConIva(producto).orElse(null)));
            }
            lineas.add(linea);
            calculadas.add(calculada);
        }
        documento.getLineas().clear();
        documento.getLineas().addAll(lineas);

        CalculoDocumento.Totales totales = CalculoDocumento.totales(calculadas);
        documento.setTotal(totales.total());
        documento.setTotalKg(totales.totalKg());
    }

    /** Todo lo que se puede editar de un documento, para saber si una edición cambió algo. */
    private String huella(Documento d) {
        StringBuilder huella = new StringBuilder()
                .append(d.getCliente().getId()).append('|')
                .append(d.getProyecto() != null ? d.getProyecto().getId() : null).append('|')
                .append(d.getFecha()).append('|')
                .append(d.getNotas()).append('|')
                .append(d.isMostrarEntrega()).append('|')
                .append(d.isMostrarTotalUsd());
        for (DocumentoLinea l : d.getLineas()) {
            huella.append('\n')
                    .append(l.getProducto().getId()).append('|')
                    .append(l.getDescripcion()).append('|')
                    .append(l.getCantidad().stripTrailingZeros().toPlainString()).append('|')
                    .append(l.getPesoUnitKg() != null ? l.getPesoUnitKg().stripTrailingZeros().toPlainString() : null).append('|')
                    .append(l.getPrecioUnit().stripTrailingZeros().toPlainString()).append('|')
                    .append(l.getMetodoPrecio()).append('|')
                    .append(l.getParametros()).append('|')
                    .append(l.getFechaEntrega()).append('|')
                    .append(l.getLugarEntrega());
        }
        return huella.toString();
    }

    private DocumentoResponse toResponse(Documento d, Documento facturaGenerada) {
        Documento origen = d.getCotizacionOrigen();
        BigDecimal totalUsd = d.getTipoCambio() != null ? CalculoDocumento.enDolares(d.getTotal(), d.getTipoCambio()) : null;
        return new DocumentoResponse(
                d.getId(),
                d.getTipo().name(),
                d.getFolio(),
                d.getEstado().name(),
                d.getFecha(),
                d.getCliente().getId(),
                d.getCliente().getNombre(),
                d.getProyecto() != null ? d.getProyecto().getId() : null,
                d.getProyecto() != null ? d.getProyecto().getNombre() : null,
                d.getNotas(),
                d.isMostrarEntrega(),
                d.getTotal(),
                d.getTotalKg(),
                d.isMostrarTotalUsd(),
                d.getTipoCambio(),
                totalUsd,
                origen != null ? origen.getId() : null,
                origen != null ? origen.getFolio() : null,
                facturaGenerada != null ? facturaGenerada.getId() : null,
                facturaGenerada != null ? facturaGenerada.getFolio() : null,
                d.getLineas().stream().map(this::toLineaResponse).toList(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private LineaResponse toLineaResponse(DocumentoLinea l) {
        return new LineaResponse(
                l.getId(),
                l.getOrden(),
                l.getProducto().getId(),
                l.getProducto().getCodigoInterno(),
                l.getProducto().isGenerico(),
                l.getProducto().getFormaCalculo().name(),
                l.getDescripcion(),
                l.getCantidad(),
                l.getPesoUnitKg(),
                l.getPesoTotalKg(),
                l.getPrecioUnit(),
                l.getTotal(),
                l.getMetodoPrecio().name(),
                l.getParametros(),
                l.getFechaEntrega(),
                l.getLugarEntrega(),
                l.getCostoUnitConIva(),
                l.getCompra() != null ? l.getCompra().getId() : null);
    }
}
