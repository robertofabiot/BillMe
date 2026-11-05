package com.example.billme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DecisionConciliacion(@NotBlank String descripcion, @NotNull UUID productoId) {
}
