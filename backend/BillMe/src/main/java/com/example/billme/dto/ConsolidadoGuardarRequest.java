package com.example.billme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsolidadoGuardarRequest(
        @NotNull UUID clienteId,
        UUID proyectoId,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta,
        List<@Valid NombreGrupo> grupos
) {
    public record NombreGrupo(
            @NotNull LocalDate fechaEntrega,
            @NotBlank @Size(max = 200) String nombre
    ) {}
}
