package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsolidadoResponse(
        UUID id,
        String folio,
        String nombre,
        UUID clienteId,
        String clienteNombre,
        UUID proyectoId,
        String proyectoNombre,
        LocalDate desde,
        LocalDate hasta,
        Instant createdAt,
        List<Grupo> grupos,
        BigDecimal total,
        BigDecimal totalKg,
        BigDecimal saldoAnterior,
        List<AbonoDelPeriodo> abonos,
        BigDecimal totalAbonos,
        BigDecimal totalActualizado
) {
    public record Grupo(
            LocalDate fechaEntrega,
            String nombre,
            List<Linea> lineas,
            BigDecimal subtotal,
            BigDecimal subtotalKg
    ) {}

    public record Linea(
            UUID documentoId,
            String folio,
            BigDecimal cantidad,
            String descripcion,
            BigDecimal pesoUnitKg,
            BigDecimal pesoTotalKg,
            BigDecimal precioUnit,
            BigDecimal total,
            String lugarEntrega
    ) {}

    public record AbonoDelPeriodo(
            LocalDate fecha,
            BigDecimal monto,
            String nota
    ) {}
}
