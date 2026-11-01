package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonoRequest(
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fechaPago
) {}
