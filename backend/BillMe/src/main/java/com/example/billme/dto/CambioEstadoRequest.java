package com.example.billme.dto;

import com.example.billme.enums.EstadoDocumento;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull EstadoDocumento estado
) {}
