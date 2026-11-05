package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GananciasResponse(
        BigDecimal facturado,
        BigDecimal ventasConCosto,
        BigDecimal costo,
        BigDecimal ganancia,
        int lineasSinCosto,
        BigDecimal ventasSinCosto,
        List<FilaGanancia> facturas
) {
    public record FilaGanancia(
            UUID id,
            String folio,
            LocalDate fecha,
            String clienteNombre,
            BigDecimal total,
            BigDecimal costo,
            BigDecimal ganancia,
            int lineasSinCosto
    ) {}
}
