package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrecioListaResponse(
        UUID id,
        BigDecimal precioSinIva,
        LocalDate fecha,
        String fuente,
        BigDecimal descuentoPct,
        BigDecimal precioOrigen,
        String nota
) {}
