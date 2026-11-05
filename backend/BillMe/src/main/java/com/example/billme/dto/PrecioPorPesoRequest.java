package com.example.billme.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PrecioPorPesoRequest(
        @NotNull @Positive BigDecimal pesoUnitKg,
        @NotNull @Positive BigDecimal tarifaUsd
) {}
