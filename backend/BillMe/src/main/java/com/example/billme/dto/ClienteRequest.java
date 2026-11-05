package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ClienteRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 30) String telefono,
        String detalles,
        @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
        @Positive BigDecimal tarifaKgUsd
) {
    public ClienteRequest(String nombre, String telefono, String detalles) {
        this(nombre, telefono, detalles, BigDecimal.ZERO, null);
    }
}
