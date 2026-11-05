package com.example.billme.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaCotizacion(
        int numeroFila,
        String codigo,
        String numFacturaDia,
        String proyecto,
        LocalDate fecha,
        BigDecimal cantidad,
        String descripcion,
        BigDecimal pesoUnitKg,
        BigDecimal precioUnit,
        BigDecimal costoSinIva,
        String observaciones,
        String entregado,
        String cliente
) {}
