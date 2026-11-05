package com.example.billme.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ConsolidadoRangoRequest(
        @NotNull UUID clienteId,
        UUID proyectoId,
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta
) {}
