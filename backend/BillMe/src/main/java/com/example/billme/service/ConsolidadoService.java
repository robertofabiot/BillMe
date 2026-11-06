package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.dto.ConsolidadoGuardarRequest;
import com.example.billme.dto.ConsolidadoRangoRequest;
import com.example.billme.dto.ConsolidadoResponse;
import com.example.billme.exportacion.ArchivoExcel;
import com.example.billme.exportacion.ConsolidadoExcel;
import com.example.billme.model.*;
import com.example.billme.repository.*;
import com.example.billme.util.Fechas;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsolidadoService {

    private final ConsolidadoRepository consolidadoRepository;
    private final ClienteRepository clienteRepository;
    private final ProyectoRepository proyectoRepository;
    private final DocumentoRepository documentoRepository;
    private final AbonoRepository abonoRepository;
    private final CuentaClienteService cuentaClienteService;
    private final ConfiguracionService configuracionService;
    private final ConsolidadoExcel consolidadoExcel;

    public ArchivoExcel exportarExcel(UUID id, String encabezado) {
        ConsolidadoResponse consolidado = obtener(id);
        byte[] contenido = consolidadoExcel.generar(consolidado, encabezado);
        String folio = consolidado.folio() != null ? consolidado.folio() : "CONS";
        String clienteSanitizado = consolidado.clienteNombre().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+", "-");
        String nombre = folio + "_" + clienteSanitizado + ".xlsx";
        return new ArchivoExcel(nombre, contenido);
    }

    public List<ConsolidadoResponse> listar(UUID clienteId) {
        List<Consolidado> lista = clienteId != null
                ? consolidadoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId)
                : consolidadoRepository.findAllByOrderByCreatedAtDesc();
        return lista.stream().map(this::toResponse).toList();
    }

    public ConsolidadoResponse obtener(UUID id) {
        Consolidado consolidado = consolidadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado no encontrado"));
        return toResponse(consolidado);
    }

    public ConsolidadoResponse vistaPrevia(ConsolidadoRangoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Proyecto proyecto = null;
        if (request.proyectoId() != null) {
            proyecto = proyectoRepository.findById(request.proyectoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        }

        return armar(null, null, "Consolidado " + cliente.getNombre(), cliente, proyecto,
                request.desde(), request.hasta(), null, Map.of(), false, configuracionService.tipoCambio().setScale(6, RoundingMode.HALF_UP));
    }

    @Transactional
    public ConsolidadoResponse crear(ConsolidadoGuardarRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Proyecto proyecto = null;
        if (request.proyectoId() != null) {
            proyecto = proyectoRepository.findById(request.proyectoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        }

        validarRangoYProyecto(cliente, proyecto, request.desde(), request.hasta());

        long seq = consolidadoRepository.siguienteFolio();
        String folio = String.format("CONS-%04d", seq);

        Consolidado consolidado = Consolidado.builder()
                .folio(folio)
                .nombre(request.nombre().trim())
                .cliente(cliente)
                .proyecto(proyecto)
                .desde(request.desde())
                .hasta(request.hasta())
                .mostrarTotalUsd(Boolean.TRUE.equals(request.mostrarTotalUsd()))
                .tipoCambio(configuracionService.tipoCambio().setScale(6, RoundingMode.HALF_UP))
                .grupos(new ArrayList<>())
                .build();

        guardarGruposEditados(consolidado, cliente, proyecto, request.desde(), request.hasta(), request.grupos());

        Consolidado guardado = consolidadoRepository.save(consolidado);
        return toResponse(guardado);
    }

    @Transactional
    public ConsolidadoResponse actualizar(UUID id, ConsolidadoGuardarRequest request) {
        Consolidado consolidado = consolidadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado no encontrado"));

        Cliente cliente = consolidado.getCliente();
        Proyecto proyecto = consolidado.getProyecto();

        validarRangoYProyecto(cliente, proyecto, request.desde(), request.hasta());

        consolidado.setNombre(request.nombre().trim());
        consolidado.setDesde(request.desde());
        consolidado.setHasta(request.hasta());
        consolidado.setMostrarTotalUsd(Boolean.TRUE.equals(request.mostrarTotalUsd()));

        consolidado.getGrupos().clear();
        consolidadoRepository.flush();
        guardarGruposEditados(consolidado, cliente, proyecto, request.desde(), request.hasta(), request.grupos());

        Consolidado guardado = consolidadoRepository.save(consolidado);
        return toResponse(guardado);
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!consolidadoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado no encontrado");
        }
        consolidadoRepository.deleteById(id);
    }

    private void guardarGruposEditados(Consolidado consolidado, Cliente cliente, Proyecto proyecto,
                                       LocalDate desde, LocalDate hasta,
                                       List<ConsolidadoGuardarRequest.NombreGrupo> gruposReq) {
        if (gruposReq == null || gruposReq.isEmpty()) return;

        ConsolidadoResponse base = armar(null, null, consolidado.getNombre(), cliente, proyecto, desde, hasta, null, Map.of(), false, configuracionService.tipoCambio().setScale(6, RoundingMode.HALF_UP));
        Map<LocalDate, String> nombresDefecto = base.grupos().stream()
                .collect(Collectors.toMap(ConsolidadoResponse.Grupo::fechaEntrega, ConsolidadoResponse.Grupo::nombre));

        for (ConsolidadoGuardarRequest.NombreGrupo ng : gruposReq) {
            if (ng.nombre() != null && !ng.nombre().isBlank()) {
                String nombreTrim = ng.nombre().trim();
                String def = nombresDefecto.get(ng.fechaEntrega());
                if (def == null || !nombreTrim.equals(def)) {
                    consolidado.getGrupos().add(ConsolidadoGrupo.builder()
                            .consolidado(consolidado)
                            .fechaEntrega(ng.fechaEntrega())
                            .nombre(nombreTrim)
                            .build());
                }
            }
        }
    }

    private void validarRangoYProyecto(Cliente cliente, Proyecto proyecto, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la final");
        }
        if (proyecto != null && !proyecto.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proyecto no pertenece a ese cliente");
        }
    }

    public ConsolidadoResponse toResponse(Consolidado c) {
        Map<LocalDate, String> nombresGuardados = c.getGrupos().stream()
                .collect(Collectors.toMap(ConsolidadoGrupo::getFechaEntrega, ConsolidadoGrupo::getNombre));
        return armar(c.getId(), c.getFolio(), c.getNombre(), c.getCliente(), c.getProyecto(),
                c.getDesde(), c.getHasta(), c.getCreatedAt(), nombresGuardados,
                c.isMostrarTotalUsd(), c.getTipoCambio());
    }

    public ConsolidadoResponse armar(UUID id, String folio, String nombreConsolidado,
                                     Cliente cliente, Proyecto proyecto,
                                     LocalDate desde, LocalDate hasta,
                                     Instant createdAt,
                                     Map<LocalDate, String> nombresGuardados,
                                     boolean mostrarTotalUsd,
                                     BigDecimal tipoCambio) {
        validarRangoYProyecto(cliente, proyecto, desde, hasta);

        LocalDate inicio = cliente.getFechaInicioCuenta() != null && cliente.getFechaInicioCuenta().isAfter(desde)
                ? cliente.getFechaInicioCuenta()
                : desde;

        List<Documento> facturas;
        List<Abono> abonos;

        if (inicio.isAfter(hasta)) {
            facturas = List.of();
            abonos = List.of();
        } else {
            UUID proyectoId = proyecto != null ? proyecto.getId() : null;
            facturas = documentoRepository.facturasDeCuenta(cliente.getId(), proyectoId, inicio, hasta);
            abonos = abonoRepository.delPeriodo(cliente.getId(), proyectoId, inicio, hasta);
        }

        record LineaSortable(LocalDate docFecha, String folio, int orden, ConsolidadoResponse.Linea linea) {}
        Map<LocalDate, List<LineaSortable>> lineasPorFecha = new TreeMap<>();

        for (Documento f : facturas) {
            for (DocumentoLinea l : f.getLineas()) {
                LocalDate fechaGrupo = l.getFechaEntrega() != null ? l.getFechaEntrega() : f.getFecha();
                BigDecimal cant = l.getCantidad();
                BigDecimal pUnit = l.getPrecioUnit();
                BigDecimal pesoUnit = l.getPesoUnitKg() != null ? l.getPesoUnitKg() : BigDecimal.ZERO;
                BigDecimal pesoTot = l.getPesoTotalKg() != null ? l.getPesoTotalKg() : pesoUnit.multiply(cant);
                BigDecimal tot = l.getTotal();
                ConsolidadoResponse.Linea dto = new ConsolidadoResponse.Linea(
                        f.getId(), f.getFolio(), cant, l.getDescripcion(), pesoUnit, pesoTot, pUnit, tot, l.getLugarEntrega()
                );
                lineasPorFecha.computeIfAbsent(fechaGrupo, k -> new ArrayList<>())
                        .add(new LineaSortable(f.getFecha(), f.getFolio(), l.getOrden(), dto));
            }
        }

        int n = 1;
        List<ConsolidadoResponse.Grupo> grupos = new ArrayList<>();
        for (Map.Entry<LocalDate, List<LineaSortable>> entry : lineasPorFecha.entrySet()) {
            LocalDate fecha = entry.getKey();
            List<LineaSortable> items = entry.getValue();
            items.sort(Comparator.comparing(LineaSortable::docFecha)
                    .thenComparing(LineaSortable::folio)
                    .thenComparingInt(LineaSortable::orden));

            List<ConsolidadoResponse.Linea> lineasDtos = items.stream().map(LineaSortable::linea).toList();
            String defaultNombre = "Subtotal " + n + " (" + Fechas.diaYMes(fecha) + ")";
            String nombre = (nombresGuardados != null && nombresGuardados.containsKey(fecha))
                    ? nombresGuardados.get(fecha)
                    : defaultNombre;

            BigDecimal subtotal = lineasDtos.stream()
                    .map(ConsolidadoResponse.Linea::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal subtotalKg = lineasDtos.stream()
                    .map(ConsolidadoResponse.Linea::pesoTotalKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            grupos.add(new ConsolidadoResponse.Grupo(fecha, nombre, lineasDtos, subtotal, subtotalKg));
            n++;
        }

        BigDecimal total = grupos.stream().map(ConsolidadoResponse.Grupo::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalKg = grupos.stream().map(ConsolidadoResponse.Grupo::subtotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ConsolidadoResponse.AbonoDelPeriodo> abonosDtos = abonos.stream()
                .map(a -> new ConsolidadoResponse.AbonoDelPeriodo(a.getFechaPago(), a.getMonto(), a.getNota()))
                .toList();
        BigDecimal totalAbonos = abonos.stream().map(Abono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID proyectoId = proyecto != null ? proyecto.getId() : null;
        BigDecimal saldoAnterior = cuentaClienteService.saldoAl(cliente.getId(), proyectoId, desde.minusDays(1));
        BigDecimal totalActualizado = saldoAnterior.add(total).subtract(totalAbonos);

        BigDecimal totalUsd = (mostrarTotalUsd && tipoCambio != null) ? CalculoDocumento.enDolares(total, tipoCambio) : null;
        BigDecimal totalActualizadoUsd = (mostrarTotalUsd && tipoCambio != null) ? CalculoDocumento.enDolares(totalActualizado, tipoCambio) : null;

        return new ConsolidadoResponse(
                id,
                folio,
                nombreConsolidado,
                cliente.getId(),
                cliente.getNombre(),
                proyectoId,
                proyecto != null ? proyecto.getNombre() : null,
                desde,
                hasta,
                createdAt,
                grupos,
                total,
                totalKg,
                saldoAnterior,
                abonosDtos,
                totalAbonos,
                totalActualizado,
                mostrarTotalUsd,
                tipoCambio,
                totalUsd,
                totalActualizadoUsd
        );
    }
}
