package com.example.billme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank @Size(max = 100) String nombre
) {}
