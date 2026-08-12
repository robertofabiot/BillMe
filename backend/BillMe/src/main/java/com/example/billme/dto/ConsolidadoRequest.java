package com.example.billme.dto;

import java.util.List;
import java.util.UUID;

public record ConsolidadoRequest(
    UUID clienteId,
    String nombre,
    List<ConsolidadoGroupRequest> grupos
) {}
