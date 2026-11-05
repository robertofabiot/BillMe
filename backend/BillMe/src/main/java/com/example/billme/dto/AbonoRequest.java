package com.example.billme.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AbonoRequest(
        @NotNull UUID clienteId,
        UUID proyectoId,
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fechaPago,
        @Size(max = 500) String nota
) {}
