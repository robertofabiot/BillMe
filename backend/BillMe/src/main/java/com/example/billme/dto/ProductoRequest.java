package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
    String codigoInterno,
    String nombrePrincipal,
    String descripcion,
    BigDecimal peso,
    BigDecimal precioListaProveedor,
    BigDecimal descuentoProveedor,
    BigDecimal costoNeto,
    List<String> aliases
) {}
