package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonoRequest(
        BigDecimal monto,
        LocalDate fechaPago
) {}
