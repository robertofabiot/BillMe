package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PrecioHistorialResponse(
    String folioInterno,
    Instant fecha,
    BigDecimal precioUnitario,
    BigDecimal cantidad
) {}
