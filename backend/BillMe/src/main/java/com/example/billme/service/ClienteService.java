package com.example.billme.service;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.model.Cliente;
import com.example.billme.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

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

    public ClienteResponse crear(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    public ClienteResponse actualizar(UUID id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setDetalles(request.detalles());
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    public void eliminar(UUID id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
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
