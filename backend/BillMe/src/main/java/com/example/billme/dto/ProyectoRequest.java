package com.example.billme.dto;

import java.util.UUID;

public record ProyectoRequest(
    UUID clienteId,
    String nombre,
    String estado
) {}
