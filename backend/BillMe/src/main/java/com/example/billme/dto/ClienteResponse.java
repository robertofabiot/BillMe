package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nombre,
        String telefono,
        String detalles,
        Instant createdAt,
        BigDecimal descuentoPct,
        BigDecimal tarifaKgUsd,
        BigDecimal saldoInicial,
        LocalDate fechaInicioCuenta,
        BigDecimal saldo
) {}
