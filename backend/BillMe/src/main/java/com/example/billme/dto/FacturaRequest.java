package com.example.billme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        UUID clienteId,
        UUID proyectoId,
        String estado,
        List<@Valid DetalleFacturaRequest> detalles
) {}
