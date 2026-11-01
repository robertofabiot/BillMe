package com.example.billme.dto;

import com.example.billme.enums.FuentePrecio;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para calcular un precio de lista.
 * FACTURA_PROVEEDOR: precio facturado sin IVA y descuento del proveedor.
 * PRECIO_CLIENTE: precio dado a un cliente (con IVA) y el descuento de ese cliente.
 * MANUAL: el precio de lista sin IVA tal cual.
 */
public record PrecioListaRequest(
        @NotNull FuentePrecio fuente,
        @NotNull @Positive BigDecimal precio,
        @DecimalMin("0") @DecimalMax("99.99") BigDecimal descuentoPct,
        LocalDate fecha,
        @Size(max = 500) String nota
) {}
