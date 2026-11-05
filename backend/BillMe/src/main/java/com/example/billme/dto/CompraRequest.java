package com.example.billme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraRequest(
        @NotNull UUID proveedorId,
        @NotNull LocalDate fecha,
        @Size(max = 50) String numeroFactura,
        String nota,
        @NotEmpty List<@Valid Linea> lineas
) {
    public record Linea(
            @NotNull UUID lineaId,
            @NotNull @Positive BigDecimal precioSinIva,
            @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
            @Positive BigDecimal pesoUnitKg
    ) {}
}
