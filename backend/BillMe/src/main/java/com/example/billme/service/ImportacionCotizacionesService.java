package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.dto.DecisionConciliacion;
import com.example.billme.dto.ImportacionCotizacionesResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.EstadoProyecto;
import com.example.billme.enums.MetodoPrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.importacion.ClaveImportacion;
import com.example.billme.importacion.CotizacionesLector;
import com.example.billme.importacion.FilaCotizacion;
import com.example.billme.importacion.LecturaCotizaciones;
import com.example.billme.model.Cliente;
import com.example.billme.model.AliasProducto;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.Producto;
import com.example.billme.model.Proyecto;
import com.example.billme.model.Usuario;
import com.example.billme.repository.AliasProductoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoLineaRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.ProyectoRepository;
import com.example.billme.util.NormalizadorTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportacionCotizacionesService {

    private final CotizacionesLector cotizacionesLector;
    private final DocumentoService documentoService;
    private final ConfiguracionService configuracionService;
    private final DocumentoRepository documentoRepository;
    private final DocumentoLineaRepository documentoLineaRepository;
    private final ClienteRepository clienteRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProductoRepository productoRepository;
    private final AliasProductoRepository aliasProductoRepository;
    private final ProductoService productoService;

    private Optional<Producto> resolverExistente(String codigo, String descripcion) {
        if (codigo != null && !codigo.isBlank()) {
            Optional<Producto> opt = productoRepository.findByCodigoInterno(codigo.trim());
            if (opt.isPresent()) {
                return opt;
            }
        }
        if (descripcion != null && !descripcion.isBlank()) {
            String norm = NormalizadorTexto.normalizar(descripcion);
            Optional<Producto> opt = productoRepository.findFirstByNombreNormAndActivoTrue(norm);
            if (opt.isPresent()) {
                return opt;
            }
            return aliasProductoRepository.findFirstByAliasNorm(norm).map(AliasProducto::getProducto);
        }
        return Optional.empty();
    }

    private String codigoPropuestaGenerica(String norm) {
        if (norm.startsWith("placa")) {
            return "GEN-PLACA";
        }
        if (norm.startsWith("platina")) {
            return "GEN-PLATINA";
        }
        return "GEN-LIBRE";
    }

    private record FilaPreparada(
            FilaCotizacion fila,
            Cliente cliente,
            String claveDoc,
            String claveLinea
    ) {}

    private record GrupoDocumento(
            String claveDoc,
            Cliente cliente,
            LocalDate fecha,
            String proyectoRaw,
            String entregadoRaw,
            List<FilaPreparada> filas
    ) {}

    public ImportacionCotizacionesResponse vistaPrevia(InputStream archivo, UUID clienteId) {
        Cliente clientePorDefecto = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        LecturaCotizaciones lectura = cotizacionesLector.leer(archivo);

        Map<String, Cliente> clientesExistentes = new HashMap<>();
        List<String> clientesNuevos = new ArrayList<>();
        Set<String> clientesNuevosSet = new HashSet<>();
        Map<String, UUID> nuevosClientesTempIds = new HashMap<>();

        Map<String, GrupoDocumento> grupos = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> ocurrenciasPorDoc = new HashMap<>();
        Set<String> todasLasClavesLinea = new HashSet<>();

        for (FilaCotizacion f : lectura.filas()) {
            String clienteColumna = (f.cliente() != null && !f.cliente().isBlank()) ? f.cliente().trim() : null;
            Cliente cliente = null;
            UUID effectiveClienteId;

            if (clienteColumna != null) {
                String nombreLower = clienteColumna.toLowerCase(Locale.ROOT);
                cliente = clientesExistentes.get(nombreLower);
                if (cliente == null && !clientesNuevosSet.contains(nombreLower)) {
                    Optional<Cliente> opt = clienteRepository.findFirstByNombreIgnoreCase(clienteColumna);
                    if (opt.isPresent()) {
                        cliente = opt.get();
                        clientesExistentes.put(nombreLower, cliente);
                    } else {
                        clientesNuevos.add(clienteColumna);
                        clientesNuevosSet.add(nombreLower);
                        UUID tempId = UUID.nameUUIDFromBytes(("nuevo-cliente:" + nombreLower).getBytes(StandardCharsets.UTF_8));
                        nuevosClientesTempIds.put(nombreLower, tempId);
                    }
                }

                if (cliente != null) {
                    effectiveClienteId = cliente.getId();
                } else {
                    effectiveClienteId = nuevosClientesTempIds.get(nombreLower);
                }
            } else {
                cliente = clientePorDefecto;
                effectiveClienteId = clientePorDefecto.getId();
            }

            String claveDoc = ClaveImportacion.de(
                    effectiveClienteId.toString(),
                    f.fecha().toString(),
                    NormalizadorTexto.normalizar(f.proyecto()),
                    f.numFacturaDia(),
                    NormalizadorTexto.normalizar(f.entregado())
            );

            String codigo = (f.codigo() != null && !f.codigo().isBlank()) ? f.codigo().trim() : null;
            String itemKey = (codigo != null) ? codigo : NormalizadorTexto.normalizar(f.descripcion());
            String cantidadStr = f.cantidad().stripTrailingZeros().toPlainString();
            String precioStr = CalculoDocumento.r2(f.precioUnit()).toPlainString();

            Map<String, Integer> ocurrenciasDoc = ocurrenciasPorDoc.computeIfAbsent(claveDoc, k -> new HashMap<>());
            String subClave = itemKey + "|" + cantidadStr + "|" + precioStr;
            int ocurrencia = ocurrenciasDoc.getOrDefault(subClave, 0);
            ocurrenciasDoc.put(subClave, ocurrencia + 1);

            String claveLinea = ClaveImportacion.de(claveDoc, itemKey, cantidadStr, precioStr, String.valueOf(ocurrencia));
            todasLasClavesLinea.add(claveLinea);

            FilaPreparada fp = new FilaPreparada(f, cliente, claveDoc, claveLinea);
            final Cliente clienteParaGrupo = cliente;
            GrupoDocumento grupo = grupos.computeIfAbsent(claveDoc, k -> new GrupoDocumento(
                    claveDoc,
                    clienteParaGrupo,
                    f.fecha(),
                    f.proyecto(),
                    f.entregado(),
                    new ArrayList<>()
            ));
            grupo.filas().add(fp);
        }

        Set<String> clavesExistentes = todasLasClavesLinea.isEmpty()
                ? Collections.emptySet()
                : documentoLineaRepository.findImportKeysIn(todasLasClavesLinea);

        int filasNuevas = 0;
        int filasYaImportadas = 0;
        int facturasNuevas = 0;
        int facturasConLineasNuevas = 0;

        for (GrupoDocumento grupo : grupos.values()) {
            long nuevasEnGrupo = 0;
            for (FilaPreparada fp : grupo.filas()) {
                if (clavesExistentes.contains(fp.claveLinea())) {
                    filasYaImportadas++;
                } else {
                    filasNuevas++;
                    nuevasEnGrupo++;
                }
            }
            if (nuevasEnGrupo > 0) {
                boolean existeDoc = documentoRepository.findByImportKey(grupo.claveDoc()).isPresent();
                if (existeDoc) {
                    facturasConLineasNuevas++;
                } else {
                    facturasNuevas++;
                }
            }
        }

        Map<String, String> descripcionesPendientes = new LinkedHashMap<>();
        Map<String, Integer> conteoPendientes = new LinkedHashMap<>();

        for (GrupoDocumento grupo : grupos.values()) {
            for (FilaPreparada fp : grupo.filas()) {
                if (clavesExistentes.contains(fp.claveLinea())) {
                    continue;
                }
                FilaCotizacion f = fp.fila();
                if (resolverExistente(f.codigo(), f.descripcion()).isEmpty()) {
                    String norm = NormalizadorTexto.normalizar(f.descripcion());
                    descripcionesPendientes.putIfAbsent(norm, f.descripcion());
                    conteoPendientes.put(norm, conteoPendientes.getOrDefault(norm, 0) + 1);
                }
            }
        }

        List<ImportacionCotizacionesResponse.PorConciliar> porConciliar = new ArrayList<>();
        for (Map.Entry<String, String> entry : descripcionesPendientes.entrySet()) {
            String norm = entry.getKey();
            String descripcion = entry.getValue();
            int filasCount = conteoPendientes.get(norm);

            String codigoGenerico = codigoPropuestaGenerica(norm);
            Producto propuesta = productoRepository.findByCodigoInterno(codigoGenerico)
                    .orElseThrow(() -> new IllegalStateException("Producto genérico " + codigoGenerico + " no encontrado"));

            List<ImportacionCotizacionesResponse.Candidato> candidatos = productoService.buscar(descripcion).stream()
                    .limit(3)
                    .map(p -> new ImportacionCotizacionesResponse.Candidato(p.id(), p.codigoInterno(), p.nombrePrincipal()))
                    .toList();

            porConciliar.add(new ImportacionCotizacionesResponse.PorConciliar(
                    descripcion,
                    filasCount,
                    propuesta.getId(),
                    propuesta.getNombrePrincipal(),
                    candidatos
            ));
        }

        return new ImportacionCotizacionesResponse(
                lectura.hoja(),
                lectura.filas().size(),
                filasNuevas,
                filasYaImportadas,
                facturasNuevas,
                facturasConLineasNuevas,
                clientesNuevos,
                lectura.incompletas(),
                porConciliar,
                false
        );
    }

    @Transactional
    public ImportacionCotizacionesResponse aplicar(InputStream archivo, UUID clienteId, List<DecisionConciliacion> decisiones) {
        Cliente clientePorDefecto = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        LecturaCotizaciones lectura = cotizacionesLector.leer(archivo);

        Map<String, Cliente> clientesMap = new HashMap<>();
        List<String> clientesNuevos = new ArrayList<>();
        Set<String> clientesNuevosSet = new HashSet<>();

        for (FilaCotizacion f : lectura.filas()) {
            String clienteColumna = (f.cliente() != null && !f.cliente().isBlank()) ? f.cliente().trim() : null;
            if (clienteColumna != null) {
                String nombreLower = clienteColumna.toLowerCase(Locale.ROOT);
                if (!clientesMap.containsKey(nombreLower)) {
                    Optional<Cliente> opt = clienteRepository.findFirstByNombreIgnoreCase(clienteColumna);
                    Cliente c;
                    if (opt.isPresent()) {
                        c = opt.get();
                    } else {
                        c = clienteRepository.save(Cliente.builder()
                                .nombre(clienteColumna)
                                .descuentoPct(BigDecimal.ZERO)
                                .build());
                        if (clientesNuevosSet.add(nombreLower)) {
                            clientesNuevos.add(clienteColumna);
                        }
                    }
                    clientesMap.put(nombreLower, c);
                }
            }
        }

        Map<String, GrupoDocumento> grupos = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> ocurrenciasPorDoc = new HashMap<>();
        Set<String> todasLasClavesLinea = new HashSet<>();

        for (FilaCotizacion f : lectura.filas()) {
            String clienteColumna = (f.cliente() != null && !f.cliente().isBlank()) ? f.cliente().trim() : null;
            Cliente cliente = (clienteColumna != null)
                    ? clientesMap.get(clienteColumna.toLowerCase(Locale.ROOT))
                    : clientePorDefecto;

            String claveDoc = ClaveImportacion.de(
                    cliente.getId().toString(),
                    f.fecha().toString(),
                    NormalizadorTexto.normalizar(f.proyecto()),
                    f.numFacturaDia(),
                    NormalizadorTexto.normalizar(f.entregado())
            );

            String codigo = (f.codigo() != null && !f.codigo().isBlank()) ? f.codigo().trim() : null;
            String itemKey = (codigo != null) ? codigo : NormalizadorTexto.normalizar(f.descripcion());
            String cantidadStr = f.cantidad().stripTrailingZeros().toPlainString();
            String precioStr = CalculoDocumento.r2(f.precioUnit()).toPlainString();

            Map<String, Integer> ocurrenciasDoc = ocurrenciasPorDoc.computeIfAbsent(claveDoc, k -> new HashMap<>());
            String subClave = itemKey + "|" + cantidadStr + "|" + precioStr;
            int ocurrencia = ocurrenciasDoc.getOrDefault(subClave, 0);
            ocurrenciasDoc.put(subClave, ocurrencia + 1);

            String claveLinea = ClaveImportacion.de(claveDoc, itemKey, cantidadStr, precioStr, String.valueOf(ocurrencia));
            todasLasClavesLinea.add(claveLinea);

            FilaPreparada fp = new FilaPreparada(f, cliente, claveDoc, claveLinea);
            GrupoDocumento grupo = grupos.computeIfAbsent(claveDoc, k -> new GrupoDocumento(
                    claveDoc,
                    cliente,
                    f.fecha(),
                    f.proyecto(),
                    f.entregado(),
                    new ArrayList<>()
            ));
            grupo.filas().add(fp);
        }

        Set<String> clavesExistentes = todasLasClavesLinea.isEmpty()
                ? Collections.emptySet()
                : documentoLineaRepository.findImportKeysIn(todasLasClavesLinea);

        int filasNuevas = 0;
        int filasYaImportadas = 0;
        int facturasNuevas = 0;
        int facturasConLineasNuevas = 0;

        for (GrupoDocumento grupo : grupos.values()) {
            long nuevasEnGrupo = 0;
            for (FilaPreparada fp : grupo.filas()) {
                if (clavesExistentes.contains(fp.claveLinea())) {
                    filasYaImportadas++;
                } else {
                    filasNuevas++;
                    nuevasEnGrupo++;
                }
            }
            if (nuevasEnGrupo > 0) {
                boolean existeDoc = documentoRepository.findByImportKey(grupo.claveDoc()).isPresent();
                if (existeDoc) {
                    facturasConLineasNuevas++;
                } else {
                    facturasNuevas++;
                }
            }
        }

        Map<String, Producto> decisionesPorTexto = new HashMap<>();
        Map<String, Producto> decisionesPorNorm = new HashMap<>();
        if (decisiones != null) {
            for (DecisionConciliacion d : decisiones) {
                Producto p = productoRepository.findById(d.productoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
                decisionesPorTexto.put(d.descripcion(), p);
                decisionesPorNorm.put(NormalizadorTexto.normalizar(d.descripcion()), p);
            }
        }

        Map<String, Producto> genericos = new HashMap<>();
        for (String cod : List.of("GEN-PLACA", "GEN-PLATINA", "GEN-LIBRE")) {
            genericos.put(cod, productoRepository.findByCodigoInterno(cod)
                    .orElseThrow(() -> new IllegalStateException("Producto genérico " + cod + " no encontrado")));
        }
        Set<String> aliasCreados = new HashSet<>();

        Map<String, Proyecto> proyectosCache = new HashMap<>();

        List<GrupoDocumento> gruposOrdenados = new ArrayList<>(grupos.values());
        gruposOrdenados.sort(Comparator.comparing(GrupoDocumento::fecha));

        for (GrupoDocumento grupo : gruposOrdenados) {
            List<FilaPreparada> lineasNuevas = grupo.filas().stream()
                    .filter(fp -> !clavesExistentes.contains(fp.claveLinea()))
                    .toList();

            if (lineasNuevas.isEmpty()) {
                continue;
            }

            Optional<Documento> docExistente = documentoRepository.findByImportKey(grupo.claveDoc());
            Documento documento;

            if (docExistente.isPresent()) {
                documento = docExistente.get();
            } else {
                Proyecto proyecto = null;
                if (grupo.proyectoRaw() != null && !grupo.proyectoRaw().isBlank()) {
                    String nombreProyecto = grupo.proyectoRaw().trim();
                    String projKey = grupo.cliente().getId().toString() + "|" + NormalizadorTexto.normalizar(nombreProyecto);
                    proyecto = proyectosCache.get(projKey);
                    if (proyecto == null) {
                        Optional<Proyecto> optProj = proyectoRepository.findFirstByClienteIdAndNombreIgnoreCase(
                                grupo.cliente().getId(), nombreProyecto);
                        if (optProj.isPresent()) {
                            proyecto = optProj.get();
                        } else {
                            proyecto = proyectoRepository.save(Proyecto.builder()
                                    .nombre(nombreProyecto)
                                    .cliente(grupo.cliente())
                                    .estado(EstadoProyecto.ACTIVO)
                                    .build());
                        }
                        proyectosCache.put(projKey, proyecto);
                    }
                }

                String folio = documentoService.siguienteFolio(TipoDocumento.FACTURA);
                Usuario usuario = documentoService.usuarioActual();

                String notas = "Importado de " + lectura.hoja() + ".";
                List<String> observaciones = grupo.filas().stream()
                        .map(fp -> fp.fila().observaciones())
                        .filter(o -> o != null && !o.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
                if (!observaciones.isEmpty()) {
                    notas = notas + " " + String.join("; ", observaciones);
                }

                documento = Documento.builder()
                        .tipo(TipoDocumento.FACTURA)
                        .folio(folio)
                        .estado(EstadoDocumento.ACTIVA)
                        .fecha(grupo.fecha())
                        .cliente(grupo.cliente())
                        .proyecto(proyecto)
                        .usuario(usuario)
                        .importKey(grupo.claveDoc())
                        .notas(notas)
                        .lineas(new ArrayList<>())
                        .build();
            }

            for (FilaPreparada fp : lineasNuevas) {
                FilaCotizacion f = fp.fila();
                Producto producto = resolverExistente(f.codigo(), f.descripcion()).orElse(null);

                if (producto == null) {
                    Producto decisionProd = decisionesPorTexto.get(f.descripcion());
                    if (decisionProd == null) {
                        decisionProd = decisionesPorNorm.get(NormalizadorTexto.normalizar(f.descripcion()));
                    }

                    if (decisionProd != null) {
                        producto = decisionProd;
                        if (!producto.isGenerico() && f.descripcion().length() <= 200) {
                            String normDesc = NormalizadorTexto.normalizar(f.descripcion());
                            if (aliasCreados.add(normDesc)) {
                                if (aliasProductoRepository.findFirstByAliasNorm(normDesc).isEmpty()) {
                                    AliasProducto alias = new AliasProducto();
                                    alias.setNombreAlias(f.descripcion());
                                    alias.setAliasNorm(normDesc);
                                    alias.setProducto(producto);
                                    aliasProductoRepository.save(alias);
                                }
                            }
                        }
                    } else {
                        String norm = NormalizadorTexto.normalizar(f.descripcion());
                        String codGen = codigoPropuestaGenerica(norm);
                        producto = genericos.get(codGen);
                    }
                }

                BigDecimal pesoUnit = f.pesoUnitKg() != null ? f.pesoUnitKg() : producto.getPeso();
                CalculoDocumento.LineaCalculada calc = CalculoDocumento.linea(f.cantidad(), f.precioUnit(), pesoUnit);

                String lugarEntrega = (f.entregado() != null && !f.entregado().isBlank())
                        ? (f.entregado().trim().length() > 200 ? f.entregado().trim().substring(0, 200) : f.entregado().trim())
                        : null;

                BigDecimal costoUnitConIva = null;
                if (f.costoSinIva() != null) {
                    costoUnitConIva = CalculoDocumento.r2(f.costoSinIva().multiply(configuracionService.factorIva()));
                }

                DocumentoLinea linea = DocumentoLinea.builder()
                        .documento(documento)
                        .orden(documento.getLineas().size())
                        .producto(producto)
                        .descripcion(f.descripcion())
                        .descripcionNorm(NormalizadorTexto.normalizar(f.descripcion()))
                        .cantidad(f.cantidad().setScale(3, RoundingMode.HALF_UP))
                        .pesoUnitKg(calc.pesoUnitKg())
                        .pesoTotalKg(calc.pesoTotalKg())
                        .precioUnit(calc.precioUnit())
                        .total(calc.total())
                        .metodoPrecio(MetodoPrecio.MANUAL)
                        .parametros(null)
                        .lugarEntrega(lugarEntrega)
                        .costoUnitConIva(costoUnitConIva)
                        .importKey(fp.claveLinea())
                        .build();

                documento.getLineas().add(linea);
            }

            List<CalculoDocumento.LineaCalculada> calculadas = documento.getLineas().stream()
                    .map(l -> new CalculoDocumento.LineaCalculada(l.getPrecioUnit(), l.getTotal(), l.getPesoUnitKg(), l.getPesoTotalKg()))
                    .toList();
            CalculoDocumento.Totales totales = CalculoDocumento.totales(calculadas);
            documento.setTotal(totales.total());
            documento.setTotalKg(totales.totalKg());
            if (documento.getLineas().stream().anyMatch(l -> l.getLugarEntrega() != null && !l.getLugarEntrega().isBlank())) {
                documento.setMostrarEntrega(true);
            }

            documentoRepository.save(documento);
        }

        return new ImportacionCotizacionesResponse(
                lectura.hoja(),
                lectura.filas().size(),
                filasNuevas,
                filasYaImportadas,
                facturasNuevas,
                facturasConLineasNuevas,
                clientesNuevos,
                lectura.incompletas(),
                List.of(),
                true
        );
    }
}
