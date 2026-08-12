package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleResponse(
        UUID id,
        UUID productoId,
        String productoNombre,
        String productoCodigoInterno,
        BigDecimal cantidad,
        BigDecimal precioUnitarioVenta,
        BigDecimal costoUnitario,
        BigDecimal pesoUnitario
) {}
