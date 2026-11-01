package com.example.billme.dto;

import java.math.BigDecimal;

public record ConfiguracionResponse(
        BigDecimal ivaPct,
        BigDecimal tipoCambio,
        BigDecimal tarifaKgUsd,
        BigDecimal densidadKgIn3,
        int diasPrecioViejo
) {}
