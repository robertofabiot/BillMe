package com.example.billme.dto;

import com.example.billme.enums.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Datos de una cotización o factura. {@code tipo} solo se usa al crear: un documento no cambia de tipo. */
public record DocumentoRequest(
        TipoDocumento tipo,
        @NotNull UUID clienteId,
        UUID proyectoId,
        @NotNull LocalDate fecha,
        String notas,
        Boolean mostrarEntrega,
        Boolean mostrarTotalUsd,
        @NotEmpty List<@Valid LineaRequest> lineas
) {
    public DocumentoRequest(TipoDocumento tipo, UUID clienteId, UUID proyectoId, LocalDate fecha,
                            String notas, Boolean mostrarEntrega, List<LineaRequest> lineas) {
        this(tipo, clienteId, proyectoId, fecha, notas, mostrarEntrega, null, lineas);
    }
}
