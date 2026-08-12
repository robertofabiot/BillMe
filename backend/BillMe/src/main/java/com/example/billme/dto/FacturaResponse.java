package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FacturaResponse(
        UUID id,
        String folioInterno,
        String estado,
        BigDecimal totalVenta,
        BigDecimal costoRealEmpresa,
        UUID clienteId,
        String clienteNombre,
        UUID proyectoId,
        String proyectoNombre,
        Instant createdAt,
        List<DetalleResponse> detalles,
        List<AbonoResponse> abonos,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente
) {}
