package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record ProyectoRequest(
    UUID clienteId,
    @Size(max = 200) String nombre,
    String estado
) {}
