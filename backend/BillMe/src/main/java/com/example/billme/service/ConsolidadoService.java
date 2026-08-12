package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.model.*;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.ConsolidadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsolidadoService {

    private final ConsolidadoRepository consolidadoRepository;
    private final ClienteRepository clienteRepository;

    public List<ConsolidadoResponse> listarTodos() {
        return consolidadoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ConsolidadoResponse> listarPorCliente(UUID clienteId) {
        return consolidadoRepository.findByClienteId(clienteId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ConsolidadoResponse obtenerPorId(UUID id) {
        return consolidadoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado no encontrado"));
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!consolidadoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consolidado no encontrado");
        }
        consolidadoRepository.deleteById(id);
    }

    @Transactional
    public ConsolidadoResponse crear(ConsolidadoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Consolidado consolidado = new Consolidado();
        consolidado.setCliente(cliente);
        consolidado.setNombre(request.nombre());
        
        long count = consolidadoRepository.count() + 1;
        consolidado.setFolioInterno(String.format("CONS-%04d", count));
        consolidado.setGrupos(new ArrayList<>());

        if (request.grupos() != null) {
            for (ConsolidadoGroupRequest groupReq : request.grupos()) {
                ConsolidadoGroup group = new ConsolidadoGroup();
                group.setNombre(groupReq.nombre());
                group.setOrden(groupReq.orden() != null ? groupReq.orden() : 0);
                group.setConsolidado(consolidado);
                group.setItems(new ArrayList<>());
                
                if (groupReq.items() != null) {
                    for (ConsolidadoItemRequest itemReq : groupReq.items()) {
                        ConsolidadoItem item = new ConsolidadoItem();
                        item.setProductoNombre(itemReq.productoNombre());
                        item.setCantidad(itemReq.cantidad());
                        item.setPrecioUnitario(itemReq.precioUnitario());
                        item.setOrden(itemReq.orden() != null ? itemReq.orden() : 0);
                        item.setGrupo(group);
                        group.getItems().add(item);
                    }
                }
                consolidado.getGrupos().add(group);
            }
        }

        Consolidado saved = consolidadoRepository.save(consolidado);
        return toResponse(saved);
    }

    private ConsolidadoResponse toResponse(Consolidado consolidado) {
        List<ConsolidadoGroupResponse> grupos = consolidado.getGrupos() != null ? 
            consolidado.getGrupos().stream().map(this::toGroupResponse).collect(Collectors.toList()) : 
            List.of();

        BigDecimal totalGeneral = grupos.stream()
                .map(ConsolidadoGroupResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoResponse(
                consolidado.getId(),
                consolidado.getFolioInterno(),
                consolidado.getNombre(),
                consolidado.getCliente() != null ? consolidado.getCliente().getId() : null,
                consolidado.getCliente() != null ? consolidado.getCliente().getNombre() : null,
                consolidado.getCreatedAt(),
                grupos,
                totalGeneral
        );
    }

    private ConsolidadoGroupResponse toGroupResponse(ConsolidadoGroup group) {
        List<ConsolidadoItemResponse> items = group.getItems() != null ?
            group.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()) :
            List.of();

        BigDecimal subtotal = items.stream()
                .map(item -> item.cantidad().multiply(item.precioUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoGroupResponse(
                group.getId(),
                group.getNombre(),
                group.getOrden(),
                items,
                subtotal
        );
    }

    private ConsolidadoItemResponse toItemResponse(ConsolidadoItem item) {
        return new ConsolidadoItemResponse(
                item.getId(),
                item.getProductoNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getOrden(),
                null
        );
    }
}
