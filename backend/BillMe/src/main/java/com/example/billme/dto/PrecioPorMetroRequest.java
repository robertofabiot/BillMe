package com.example.billme.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PrecioPorMetroRequest(
        @NotNull @Positive BigDecimal precioReferencia,
        @NotNull @Positive BigDecimal largoReferencia,
        @NotNull @Positive BigDecimal largoNuevo
) {}
