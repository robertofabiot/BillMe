package com.example.billme.dto;

import com.example.billme.enums.MonedaTarifa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PrecioPorPesoRequest(
        @NotNull @Positive BigDecimal pesoUnitKg,
        @NotNull @Positive BigDecimal tarifa,
        @NotNull MonedaTarifa moneda,
        @Positive BigDecimal tipoCambio
) {}
