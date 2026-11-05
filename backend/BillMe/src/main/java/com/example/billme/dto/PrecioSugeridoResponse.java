package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrecioSugeridoResponse(
        BigDecimal precioUnit,
        String origen,
        String clienteNombre,
        String folio,
        LocalDate fecha,
        Long diasAntiguedad,
        boolean viejo,
        BigDecimal precioLista,
        BigDecimal descuentoPct,
        BigDecimal precioFormula,
        BigDecimal tarifaKgUsd,
        String origenTarifa
) {}
