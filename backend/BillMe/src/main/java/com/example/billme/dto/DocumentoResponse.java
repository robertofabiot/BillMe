package com.example.billme.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentoResponse(
        UUID id,
        String tipo,
        String folio,
        String estado,
        LocalDate fecha,
        UUID clienteId,
        String clienteNombre,
        UUID proyectoId,
        String proyectoNombre,
        String notas,
        boolean mostrarEntrega,
        BigDecimal total,
        BigDecimal totalKg,
        /** En una factura: la cotización de la que salió. */
        UUID cotizacionOrigenId,
        String cotizacionOrigenFolio,
        /** En una cotización aceptada: la factura que generó (solo al consultar un documento). */
        UUID facturaGeneradaId,
        String facturaGeneradaFolio,
        List<LineaResponse> lineas,
        Instant createdAt,
        Instant updatedAt
) {}
