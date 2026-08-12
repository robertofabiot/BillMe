package com.example.billme.dto;

import java.math.BigDecimal;

public record ReporteResponse(
    BigDecimal ingresosTotales,
    BigDecimal totalCobrado,
    BigDecimal porCobrar,
    BigDecimal costoTotal,
    BigDecimal gananciaNeta,
    int totalFacturas
) {}
