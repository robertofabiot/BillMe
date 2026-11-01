package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ConfiguracionRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ivaPct,
        @NotNull @Positive BigDecimal tipoCambio,
        @NotNull @Positive BigDecimal tarifaKgUsd,
        @NotNull @Positive BigDecimal densidadKgIn3,
        @NotNull @Min(1) @Max(3650) Integer diasPrecioViejo
) {}
