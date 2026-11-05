package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 30) String telefono,
        String detalles,
        @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
        @Positive BigDecimal tarifaKgUsd,
        BigDecimal saldoInicial,
        LocalDate fechaInicioCuenta
) {
    public ClienteRequest(String nombre, String telefono, String detalles) {
        this(nombre, telefono, detalles, BigDecimal.ZERO, null, BigDecimal.ZERO, null);
    }

    public ClienteRequest(String nombre, String telefono, String detalles, BigDecimal descuentoPct, BigDecimal tarifaKgUsd) {
        this(nombre, telefono, detalles, descuentoPct, tarifaKgUsd, BigDecimal.ZERO, null);
    }
}
