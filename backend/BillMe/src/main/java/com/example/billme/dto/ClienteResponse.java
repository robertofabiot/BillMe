package com.example.billme.dto;

import java.time.Instant;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nombre,
        String telefono,
        String detalles,
        Instant createdAt
) {}
