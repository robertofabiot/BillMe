package com.example.billme.service;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.model.Abono;
import com.example.billme.model.Cliente;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbonoService {

    private final AbonoRepository abonoRepository;
    private final ClienteRepository clienteRepository;
    private final ProyectoRepository proyectoRepository;

    @Transactional
    public AbonoResponse registrarAbono(AbonoRequest request) {
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

        Abono abono = Abono.builder()
                .cliente(cliente)
                .proyecto(proyecto)
                .monto(request.monto())
                .fechaPago(request.fechaPago())
                .nota(request.nota())
                .build();

        return toResponse(abonoRepository.save(abono));
    }

    @Transactional
    public AbonoResponse actualizarAbono(UUID id, AbonoRequest request) {
        Abono abono = abonoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Abono no encontrado"));

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

        abono.setCliente(cliente);
        abono.setProyecto(proyecto);
        abono.setMonto(request.monto());
        abono.setFechaPago(request.fechaPago());
        abono.setNota(request.nota());

        return toResponse(abonoRepository.save(abono));
    }

    @Transactional
    public void eliminarAbono(UUID id) {
        if (!abonoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Abono no encontrado");
        }
        abonoRepository.deleteById(id);
    }

    private AbonoResponse toResponse(Abono a) {
        String proyectoNombre = a.getProyecto() != null ? a.getProyecto().getNombre() : null;
        UUID proyectoId = a.getProyecto() != null ? a.getProyecto().getId() : null;
        return new AbonoResponse(
                a.getId(),
                a.getCliente().getId(),
                proyectoId,
                proyectoNombre,
                a.getMonto(),
                a.getFechaPago(),
                a.getNota()
        );
    }
}
