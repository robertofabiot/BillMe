package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.List;

/** Precio de lista vigente de un producto, su historial y el descuento sugerido para calcular uno nuevo. */
public record PreciosListaProductoResponse(
        PrecioListaResponse vigente,
        List<PrecioListaResponse> historial,
        BigDecimal descuentoProveedorSugerido
) {}
