package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsolidadoItemResponse(
    UUID id,
    String productoNombre,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    Integer orden,
    UUID facturaId
) {}
