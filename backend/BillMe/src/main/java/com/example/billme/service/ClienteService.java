package com.example.billme.service;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.model.Cliente;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.ConsolidadoRepository;
import com.example.billme.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DocumentoRepository documentoRepository;
    private final ConsolidadoRepository consolidadoRepository;
    private final AbonoRepository abonoRepository;
    private final CuentaClienteService cuentaClienteService;

    public List<ClienteResponse> listarTodos() {
        Map<UUID, BigDecimal> saldos = cuentaClienteService.saldos();
        return clienteRepository.findAll().stream()
                .map(c -> toResponse(c, saldos.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public List<ClienteResponse> buscar(String q) {
        Map<UUID, BigDecimal> saldos = cuentaClienteService.saldos();
        return clienteRepository.buscar(q).stream()
                .map(c -> toResponse(c, saldos.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public ClienteResponse obtenerPorId(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        return toResponse(cliente, cuentaClienteService.cuenta(id).saldo());
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente.setDescuentoPct(request.descuentoPct() != null ? request.descuentoPct() : BigDecimal.ZERO);
        cliente.setTarifaKgUsd(request.tarifaKgUsd());
        cliente.setSaldoInicial(request.saldoInicial() != null ? request.saldoInicial() : BigDecimal.ZERO);
        cliente.setFechaInicioCuenta(request.fechaInicioCuenta());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente, cliente.getSaldoInicial());
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ClienteRequest request) {
        if (request.fechaInicioCuenta() != null
                && abonoRepository.existsByClienteIdAndFechaPagoBefore(id, request.fechaInicioCuenta())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hay abonos anteriores a esa fecha: cámbialos antes de mover el inicio de la cuenta");
        }
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente.setDescuentoPct(request.descuentoPct() != null ? request.descuentoPct() : BigDecimal.ZERO);
        cliente.setTarifaKgUsd(request.tarifaKgUsd());
        cliente.setSaldoInicial(request.saldoInicial() != null ? request.saldoInicial() : BigDecimal.ZERO);
        cliente.setFechaInicioCuenta(request.fechaInicioCuenta());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente, cuentaClienteService.cuenta(id).saldo());
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        // El historial de ventas nunca se borra junto con el cliente
        if (documentoRepository.existsByClienteId(id) || consolidadoRepository.existsByClienteId(id)
                || abonoRepository.existsByClienteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un cliente que tiene cotizaciones, facturas, consolidados o abonos");
        }
        clienteRepository.deleteById(id);
    }

    private ClienteResponse toResponse(Cliente entity, BigDecimal saldo) {
        return new ClienteResponse(
                entity.getId(),
                entity.getNombre(),
                entity.getTelefono(),
                entity.getDetalles(),
                entity.getCreatedAt(),
                entity.getDescuentoPct(),
                entity.getTarifaKgUsd(),
                entity.getSaldoInicial(),
                entity.getFechaInicioCuenta(),
                saldo
        );
    }
}
