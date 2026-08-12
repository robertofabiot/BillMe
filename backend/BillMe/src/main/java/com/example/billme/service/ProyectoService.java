package com.example.billme.service;

import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.ProyectoResponse;
import com.example.billme.model.Cliente;
import com.example.billme.enums.EstadoProyecto;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final ClienteRepository clienteRepository;

    public List<ProyectoResponse> listarTodos() {
        return proyectoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProyectoResponse> listarPorCliente(UUID clienteId) {
        return proyectoRepository.findByClienteId(clienteId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProyectoResponse> listarActivosPorCliente(UUID clienteId) {
        return proyectoRepository.findByClienteIdAndEstado(clienteId, EstadoProyecto.ACTIVO).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProyectoResponse obtenerPorId(UUID id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        return toResponse(proyecto);
    }

    public ProyectoResponse crear(ProyectoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        
        Proyecto proyecto = new Proyecto();
        proyecto.setCliente(cliente);
        proyecto.setNombre(request.nombre());
        
        if (request.estado() != null) {
            proyecto.setEstado(EstadoProyecto.valueOf(request.estado()));
        } else {
            proyecto.setEstado(EstadoProyecto.ACTIVO);
        }
        
        Proyecto saved = proyectoRepository.save(proyecto);
        return toResponse(saved);
    }

    public ProyectoResponse actualizar(UUID id, ProyectoRequest request) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        
        if (request.nombre() != null) {
            proyecto.setNombre(request.nombre());
        }
        
        if (request.estado() != null) {
            proyecto.setEstado(EstadoProyecto.valueOf(request.estado()));
        }
        
        if (request.clienteId() != null && !proyecto.getCliente().getId().equals(request.clienteId())) {
            Cliente cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
            proyecto.setCliente(cliente);
        }
        
        Proyecto updated = proyectoRepository.save(proyecto);
        return toResponse(updated);
    }

    private ProyectoResponse toResponse(Proyecto entity) {
        return new ProyectoResponse(
                entity.getId(),
                entity.getCliente().getId(),
                entity.getCliente().getNombre(),
                entity.getNombre(),
                entity.getEstado() != null ? entity.getEstado().name() : null,
                entity.getCreatedAt()
        );
    }
}
