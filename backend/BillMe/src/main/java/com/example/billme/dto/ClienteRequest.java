package com.example.billme.dto;

import com.example.billme.enums.MonedaTarifa;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 30) String telefono,
        String detalles,
        @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
        @Positive @DecimalMax("9999.9999") BigDecimal tarifaKg,
        MonedaTarifa tarifaMoneda,
        BigDecimal saldoInicial,
        LocalDate fechaInicioCuenta
) {
    public ClienteRequest(String nombre, String telefono, String detalles) {
        this(nombre, telefono, detalles, BigDecimal.ZERO, null, null, BigDecimal.ZERO, null);
    }

    public ClienteRequest(String nombre, String telefono, String detalles, BigDecimal descuentoPct, BigDecimal tarifaKg) {
        this(nombre, telefono, detalles, descuentoPct, tarifaKg, null, BigDecimal.ZERO, null);
    }

    public ClienteRequest(String nombre, String telefono, String detalles, BigDecimal descuentoPct, BigDecimal tarifaKg,
                          BigDecimal saldoInicial, LocalDate fechaInicioCuenta) {
        this(nombre, telefono, detalles, descuentoPct, tarifaKg, null, saldoInicial, fechaInicioCuenta);
    }
}
