package com.example.billme.dto;

import java.math.BigDecimal;

/** Resultado de calcular un precio de lista sin guardarlo. */
public record CalculoListaResponse(
        BigDecimal precioSinIva
) {}
