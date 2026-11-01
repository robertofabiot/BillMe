package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record DetalleFacturaRequest(
        @NotNull UUID productoId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @PositiveOrZero BigDecimal precioUnitarioVenta
) {}
