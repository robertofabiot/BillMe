package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CuentaClienteResponse(
        UUID clienteId,
        String clienteNombre,
        BigDecimal saldoInicial,
        LocalDate fechaInicioCuenta,
        BigDecimal totalFacturado,
        BigDecimal totalAbonado,
        BigDecimal saldo,
        List<SaldoProyecto> proyectos,
        List<Movimiento> movimientos
) {
    /** proyectoId null = "Sin proyecto". */
    public record SaldoProyecto(
            UUID proyectoId,
            String nombre,
            BigDecimal facturado,
            BigDecimal abonado,
            BigDecimal saldo
    ) {}

    /** tipo: SALDO_INICIAL, FACTURA o ABONO. id: de la factura o del abono. concepto: folio, nota o "Saldo inicial". */
    public record Movimiento(
            LocalDate fecha,
            String tipo,
            UUID id,
            String concepto,
            String proyectoNombre,
            BigDecimal cargo,
            BigDecimal abono,
            BigDecimal saldo
    ) {}
}
