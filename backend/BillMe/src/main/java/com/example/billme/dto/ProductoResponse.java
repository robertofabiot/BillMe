package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductoResponse(
    UUID id,
    String codigoInterno,
    String nombrePrincipal,
    String descripcion,
    BigDecimal peso,
    BigDecimal precioListaProveedor,
    BigDecimal descuentoProveedor,
    BigDecimal costoNeto,
    List<String> aliases,
    Instant createdAt
) {}
