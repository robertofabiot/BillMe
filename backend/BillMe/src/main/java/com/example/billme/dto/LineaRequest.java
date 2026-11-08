package com.example.billme.dto;

import com.example.billme.enums.MetodoPrecio;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Línea de una cotización o factura.
 * {@code id} viene al editar una línea existente. Sin descripción se usa el nombre del
 * producto; sin peso, el del catálogo. El total lo calcula el backend.
 */
public record LineaRequest(
        UUID id,
        @NotNull UUID productoId,
        @Size(max = 500) String descripcion,
        @NotNull @Positive @DecimalMax("9999999.999") BigDecimal cantidad,
        @PositiveOrZero @DecimalMax("99999999.99") BigDecimal pesoUnitKg,
        @NotNull @PositiveOrZero @DecimalMax("9999999999.99") BigDecimal precioUnit,
        MetodoPrecio metodoPrecio,
        String parametros,
        LocalDate fechaEntrega,
        @Size(max = 200) String lugarEntrega
) {}
