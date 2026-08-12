package com.example.billme.dto;

import java.time.Instant;
import java.util.UUID;

public record ProyectoResponse(
    UUID id,
    UUID clienteId,
    String clienteNombre,
    String nombre,
    String estado,
    Instant createdAt
) {}
