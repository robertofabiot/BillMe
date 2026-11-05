package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrecioHistorialResponse(
        UUID documentoId,
        String folio,
        String tipo,
        LocalDate fecha,
        String clienteNombre,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnit,
        boolean viejo
) {}
