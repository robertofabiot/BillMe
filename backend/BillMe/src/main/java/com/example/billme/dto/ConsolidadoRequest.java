package com.example.billme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record ConsolidadoRequest(
    @NotNull UUID clienteId,
    @NotBlank @Size(max = 200) String nombre,
    List<@Valid ConsolidadoGroupRequest> grupos
) {}
