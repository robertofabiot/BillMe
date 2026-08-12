package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleFacturaRequest(
        UUID productoId,
        BigDecimal cantidad,
        BigDecimal precioUnitarioVenta
) {}
