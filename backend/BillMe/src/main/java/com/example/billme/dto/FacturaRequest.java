package com.example.billme.dto;

import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        UUID clienteId,
        UUID proyectoId,
        String estado,
        List<DetalleFacturaRequest> detalles
) {}
