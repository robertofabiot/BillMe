package com.example.billme.dto;

import java.math.BigDecimal;

public record PrecioCalculadoResponse(
        BigDecimal precioUnit
) {}
