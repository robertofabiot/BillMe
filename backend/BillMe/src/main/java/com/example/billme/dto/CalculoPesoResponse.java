package com.example.billme.dto;

import java.math.BigDecimal;

public record CalculoPesoResponse(
        BigDecimal pesoUnitKg,
        String descripcion
) {}
