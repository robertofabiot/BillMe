package com.example.billme.service;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.model.Cliente;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.ConsolidadoRepository;
import com.example.billme.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DocumentoRepository documentoRepository;
    private final ConsolidadoRepository consolidadoRepository;

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ClienteResponse> buscar(String q) {
        return clienteRepository.buscar(q).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ClienteResponse obtenerPorId(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        // El historial de ventas nunca se borra junto con el cliente
        if (documentoRepository.existsByClienteId(id) || consolidadoRepository.existsByClienteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un cliente que tiene cotizaciones, facturas o consolidados");
        }
        clienteRepository.deleteById(id);
    }

    private ClienteResponse toResponse(Cliente entity) {
        return new ClienteResponse(
                entity.getId(),
                entity.getNombre(),
                entity.getTelefono(),
                entity.getDetalles(),
                entity.getCreatedAt()
        );
    }
}
