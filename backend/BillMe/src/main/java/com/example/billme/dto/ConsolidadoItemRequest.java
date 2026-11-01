package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ConsolidadoItemRequest(
    @NotBlank @Size(max = 200) String productoNombre,
    @NotNull @Positive BigDecimal cantidad,
    @NotNull @PositiveOrZero BigDecimal precioUnitario,
    Integer orden,
    UUID facturaId
) {}
