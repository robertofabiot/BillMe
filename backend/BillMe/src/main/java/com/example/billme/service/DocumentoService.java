package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.calculo.CalculoDocumento.LineaCalculada;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.LineaResponse;
import com.example.billme.dto.RevisionResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.MetodoPrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Abono;
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
import lombok.RequiredArgsConstructor;
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
    private final JsonMapper jsonMapper;

    public List<DocumentoResponse> filtrar(TipoDocumento tipo, EstadoDocumento estado, UUID clienteId, UUID proyectoId,
                                           UUID productoId, LocalDate desde, LocalDate hasta) {
        return documentoRepository.filtrar(tipo, estado, clienteId, proyectoId, productoId, desde, hasta).stream()
                .map(d -> toResponse(d, null))
                .toList();
    }

    public DocumentoResponse obtener(UUID id) {
        Documento documento = buscar(id);
        return toResponse(documento, documentoRepository.findFirstByCotizacionOrigenId(id).orElse(null));
    }

    @Transactional
    public DocumentoResponse crear(DocumentoRequest request) {
        if (request.tipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica si es cotización o factura");
        }
        Documento documento = Documento.builder()
                .tipo(request.tipo())
                .estado(request.tipo() == TipoDocumento.COTIZACION ? EstadoDocumento.PENDIENTE : EstadoDocumento.ACTIVA)
                .folio(siguienteFolio(request.tipo()))
                .usuario(usuarioActual())
                .build();
        aplicar(documento, request);
        return toResponse(documentoRepository.saveAndFlush(documento), null);
    }

    @Transactional
    public DocumentoResponse actualizar(UUID id, DocumentoRequest request) {
        Documento documento = buscar(id);
        if (documento.getEstado() == EstadoDocumento.ANULADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Una factura anulada no se puede editar");
        }
        if (documento.getEstado() == EstadoDocumento.ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta cotización ya se convirtió en factura: edita la factura");
        }
        String copiaAnterior = jsonMapper.writeValueAsString(toResponse(documento, null));
        String antes = huella(documento);
        aplicar(documento, request);
        // Guardar sin cambios no deja una versión nueva
        if (!huella(documento).equals(antes)) {
            revisionRepository.save(DocumentoRevision.builder()
                    .documento(documento)
                    .numero(revisionRepository.countByDocumentoId(id) + 1)
                    .snapshot(copiaAnterior)
                    .usuario(usuarioActual())
                    .build());
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
        Documento factura = Documento.builder()
                .tipo(TipoDocumento.FACTURA)
                .estado(EstadoDocumento.ACTIVA)
                .folio(siguienteFolio(TipoDocumento.FACTURA))
                .fecha(fecha != null ? fecha : Fechas.hoy())
                .cliente(cotizacion.getCliente())
                .proyecto(cotizacion.getProyecto())
                .notas(cotizacion.getNotas())
                .mostrarEntrega(cotizacion.isMostrarEntrega())
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
        return toResponse(documentoRepository.saveAndFlush(factura), null);
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
        documento.setEstado(nuevo);
        return toResponse(documento, null);
    }

    /** Una factura anulada deja de contar como ingreso y como precio dado, pero se conserva. */
    @Transactional
    public DocumentoResponse anular(UUID id) {
        Documento documento = buscar(id);
        if (documento.getTipo() != TipoDocumento.FACTURA || documento.getEstado() != EstadoDocumento.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede anular una factura activa");
        }
        if (!documento.getAbonos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede anular una factura con abonos registrados");
        }
        documento.setEstado(EstadoDocumento.ANULADA);
        return toResponse(documento, null);
    }

    private Documento buscar(UUID id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    private String siguienteFolio(TipoDocumento tipo) {
        return tipo == TipoDocumento.COTIZACION
                ? "COT-%04d".formatted(documentoRepository.siguienteFolioCotizacion())
                : "FAC-%04d".formatted(documentoRepository.siguienteFolioFactura());
    }

    /** El usuario en sesión; null si no corresponde a un usuario guardado. */
    private Usuario usuarioActual() {
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
            LineaCalculada calculada = CalculoDocumento.linea(cantidad, pedida.precioUnit(),
                    pedida.pesoUnitKg() != null ? pedida.pesoUnitKg() : producto.getPeso());

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
                .append(d.isMostrarEntrega());
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
        BigDecimal abonado = d.getAbonos().stream().map(Abono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        Documento origen = d.getCotizacionOrigen();
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
                origen != null ? origen.getId() : null,
                origen != null ? origen.getFolio() : null,
                facturaGenerada != null ? facturaGenerada.getId() : null,
                facturaGenerada != null ? facturaGenerada.getFolio() : null,
                d.getLineas().stream().map(this::toLineaResponse).toList(),
                d.getAbonos().stream().map(a -> new AbonoResponse(a.getId(), a.getMonto(), a.getFechaPago())).toList(),
                abonado,
                d.getTipo() == TipoDocumento.FACTURA ? d.getTotal().subtract(abonado) : null,
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
                l.getCostoUnitConIva());
    }
}
