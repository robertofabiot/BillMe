package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
    @NotBlank @Size(max = 50) String codigoInterno,
    @NotBlank @Size(max = 200) String nombrePrincipal,
    String descripcion,
    @PositiveOrZero BigDecimal peso,
    @PositiveOrZero BigDecimal precioListaProveedor,
    @DecimalMin("0") @DecimalMax("100") BigDecimal descuentoProveedor,
    @PositiveOrZero BigDecimal costoNeto,
    List<@NotBlank @Size(max = 200) String> aliases
) {}
