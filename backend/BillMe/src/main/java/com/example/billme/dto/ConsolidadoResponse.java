package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsolidadoResponse(
    UUID id,
    String folioInterno,
    String nombre,
    UUID clienteId,
    String clienteNombre,
    Instant createdAt,
    List<ConsolidadoGroupResponse> grupos,
    BigDecimal totalGeneral
) {}
