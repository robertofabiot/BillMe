package com.example.billme.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
    @NotBlank @Size(max = 50) String codigoInterno,
    @NotBlank @Size(max = 200) String nombrePrincipal,
    String descripcion,
    @Size(max = 20) String unidad,
    @PositiveOrZero BigDecimal peso,
    List<@NotBlank @Size(max = 200) String> aliases
) {}
