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
        List<@Valid NombreGrupo> grupos,
        Boolean mostrarTotalUsd
) {
    public ConsolidadoGuardarRequest(UUID clienteId, UUID proyectoId, String nombre,
                                     LocalDate desde, LocalDate hasta, List<NombreGrupo> grupos) {
        this(clienteId, proyectoId, nombre, desde, hasta, grupos, null);
    }

    public record NombreGrupo(
            @NotNull LocalDate fechaEntrega,
            @NotBlank @Size(max = 200) String nombre
    ) {}
}
