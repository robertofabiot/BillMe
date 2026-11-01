package com.example.billme.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductoResponse(
    UUID id,
    String codigoInterno,
    String nombrePrincipal,
    String descripcion,
    String unidad,
    String familia,
    BigDecimal peso,
    boolean generico,
    String formaCalculo,
    /** Precio de lista del proveedor sin IVA más reciente; null si no hay. */
    BigDecimal precioListaVigente,
    LocalDate fechaPrecioLista,
    List<String> aliases,
    Instant createdAt
) {}
