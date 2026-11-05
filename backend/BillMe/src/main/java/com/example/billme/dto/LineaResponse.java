package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LineaResponse(
        UUID id,
        int orden,
        UUID productoId,
        String productoCodigo,
        boolean productoGenerico,
        String productoForma,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal pesoUnitKg,
        BigDecimal pesoTotalKg,
        BigDecimal precioUnit,
        BigDecimal total,
        String metodoPrecio,
        String parametros,
        LocalDate fechaEntrega,
        String lugarEntrega,
        BigDecimal costoUnitConIva,
        UUID compraId
) {}
