package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AbonoResponse(
        UUID id,
        BigDecimal monto,
        LocalDate fechaPago
) {}
