package com.example.billme.service;

import com.example.billme.dto.CuentaClienteResponse;
import com.example.billme.dto.CuentaClienteResponse.Movimiento;
import com.example.billme.dto.CuentaClienteResponse.SaldoProyecto;
import com.example.billme.model.Abono;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.SumaPorCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentaClienteService {

    private final ClienteRepository clienteRepository;
    private final DocumentoRepository documentoRepository;
    private final AbonoRepository abonoRepository;

    public CuentaClienteResponse cuenta(UUID clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        LocalDate desde = cliente.getFechaInicioCuenta();
        BigDecimal saldoInicial = cliente.getSaldoInicial() != null ? cliente.getSaldoInicial() : BigDecimal.ZERO;

        List<Documento> facturas = documentoRepository.facturasDeCuenta(clienteId, null, desde, null);
        List<Abono> abonos = abonoRepository.findByClienteIdOrderByFechaPagoAscCreatedAtAsc(clienteId).stream()
                .filter(a -> desde == null || !a.getFechaPago().isBefore(desde))
                .toList();

        BigDecimal totalFacturado = facturas.stream().map(Documento::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAbonado = abonos.stream().map(Abono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = saldoInicial.add(totalFacturado).subtract(totalAbonado);

        List<Movimiento> movimientos = new ArrayList<>();
        BigDecimal saldoCorrido = BigDecimal.ZERO;

        if (saldoInicial.compareTo(BigDecimal.ZERO) != 0) {
            saldoCorrido = saldoInicial;
            BigDecimal cargo = saldoInicial.compareTo(BigDecimal.ZERO) > 0 ? saldoInicial : null;
            BigDecimal abono = saldoInicial.compareTo(BigDecimal.ZERO) < 0 ? saldoInicial.negate() : null;
            movimientos.add(new Movimiento(desde, "SALDO_INICIAL", null, "Saldo inicial", null, cargo, abono, saldoCorrido));
        }

        record ItemMov(LocalDate fecha, int tipoPrioridad, Instant createdAt, MovimientoFactory factory) {}

        List<ItemMov> items = new ArrayList<>();
        for (Documento f : facturas) {
            items.add(new ItemMov(f.getFecha(), 0, f.getCreatedAt(), new MovimientoFactory() {
                @Override
                public Movimiento crear(BigDecimal saldo) {
                    String proyectoNombre = f.getProyecto() != null ? f.getProyecto().getNombre() : null;
                    return new Movimiento(f.getFecha(), "FACTURA", f.getId(), f.getFolio(), proyectoNombre,
                            f.getTotal(), null, saldo);
                }
                @Override
                public BigDecimal delta() {
                    return f.getTotal();
                }
            }));
        }
        for (Abono a : abonos) {
            items.add(new ItemMov(a.getFechaPago(), 1, a.getCreatedAt(), new MovimientoFactory() {
                @Override
                public Movimiento crear(BigDecimal saldo) {
                    String proyectoNombre = a.getProyecto() != null ? a.getProyecto().getNombre() : null;
                    String concepto = (a.getNota() != null && !a.getNota().isBlank()) ? a.getNota().trim() : "Abono";
                    return new Movimiento(a.getFechaPago(), "ABONO", a.getId(), concepto, proyectoNombre,
                            null, a.getMonto(), saldo);
                }
                @Override
                public BigDecimal delta() {
                    return a.getMonto().negate();
                }
            }));
        }

        items.sort(Comparator.comparing(ItemMov::fecha)
                .thenComparingInt(ItemMov::tipoPrioridad)
                .thenComparing(ItemMov::createdAt, Comparator.nullsLast(Comparator.naturalOrder())));

        for (ItemMov it : items) {
            saldoCorrido = saldoCorrido.add(it.factory().delta());
            movimientos.add(it.factory().crear(saldoCorrido));
        }

        List<SaldoProyecto> saldoProyectos = new ArrayList<>();
        if (cliente.getProyectos() != null && !cliente.getProyectos().isEmpty()) {
            for (Proyecto p : cliente.getProyectos()) {
                BigDecimal fac = facturas.stream()
                        .filter(f -> f.getProyecto() != null && f.getProyecto().getId().equals(p.getId()))
                        .map(Documento::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal abo = abonos.stream()
                        .filter(a -> a.getProyecto() != null && a.getProyecto().getId().equals(p.getId()))
                        .map(Abono::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                saldoProyectos.add(new SaldoProyecto(p.getId(), p.getNombre(), fac, abo, fac.subtract(abo)));
            }
            BigDecimal facSin = facturas.stream()
                    .filter(f -> f.getProyecto() == null)
                    .map(Documento::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal aboSin = abonos.stream()
                    .filter(a -> a.getProyecto() == null)
                    .map(Abono::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal saldoSin = saldoInicial.add(facSin).subtract(aboSin);
            saldoProyectos.add(new SaldoProyecto(null, "Sin proyecto", facSin, aboSin, saldoSin));
        }

        return new CuentaClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                saldoInicial,
                desde,
                totalFacturado,
                totalAbonado,
                saldo,
                saldoProyectos,
                movimientos
        );
    }

    public BigDecimal saldoAl(UUID clienteId, UUID proyectoId, LocalDate fecha) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        LocalDate desde = cliente.getFechaInicioCuenta();
        List<Documento> facturas = documentoRepository.facturasDeCuenta(clienteId, proyectoId, desde, fecha);
        BigDecimal facturado = facturas.stream().map(Documento::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonado = abonoRepository.sumar(clienteId, proyectoId, desde, fecha);
        if (proyectoId == null) {
            BigDecimal saldoInicial = cliente.getSaldoInicial() != null ? cliente.getSaldoInicial() : BigDecimal.ZERO;
            return saldoInicial.add(facturado).subtract(abonado);
        } else {
            return facturado.subtract(abonado);
        }
    }

    public Map<UUID, BigDecimal> saldos() {
        List<Cliente> clientes = clienteRepository.findAll();
        Map<UUID, BigDecimal> facturas = documentoRepository.sumaFacturasPorCliente().stream()
                .collect(Collectors.toMap(SumaPorCliente::getClienteid, SumaPorCliente::getSuma));
        Map<UUID, BigDecimal> abonos = abonoRepository.sumaAbonosPorCliente().stream()
                .collect(Collectors.toMap(SumaPorCliente::getClienteid, SumaPorCliente::getSuma));

        Map<UUID, BigDecimal> resultado = new HashMap<>();
        for (Cliente c : clientes) {
            BigDecimal ini = c.getSaldoInicial() != null ? c.getSaldoInicial() : BigDecimal.ZERO;
            BigDecimal f = facturas.getOrDefault(c.getId(), BigDecimal.ZERO);
            BigDecimal a = abonos.getOrDefault(c.getId(), BigDecimal.ZERO);
            resultado.put(c.getId(), ini.add(f).subtract(a));
        }
        return resultado;
    }

    private interface MovimientoFactory {
        Movimiento crear(BigDecimal saldo);
        BigDecimal delta();
    }
}
