package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrecioHistorialResponse(
    String folio,
    LocalDate fecha,
    BigDecimal precioUnitario,
    BigDecimal cantidad
) {}
