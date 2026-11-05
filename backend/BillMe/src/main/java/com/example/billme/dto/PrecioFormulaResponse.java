package com.example.billme.dto;

import java.math.BigDecimal;

public record PrecioFormulaResponse(
        BigDecimal precioLista,
        BigDecimal descuentoPct,
        BigDecimal precioUnit
) {}
