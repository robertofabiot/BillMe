package com.example.billme.dto;

import jakarta.validation.constraints.*;
public record ClienteRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 30) String telefono,
        String detalles
) {}
