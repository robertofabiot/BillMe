package com.example.billme.dto;

import java.util.List;
import java.util.UUID;

public record ImportacionCotizacionesResponse(
        String hoja,
        int filasLeidas,
        int filasNuevas,
        int filasYaImportadas,
        int facturasNuevas,
        int facturasConLineasNuevas,
        List<String> clientesNuevos,
        List<FilaProblema> filasIncompletas,
        List<PorConciliar> porConciliar,
        boolean aplicada) {

    public record PorConciliar(String descripcion, int filas, UUID propuestaProductoId,
                               String propuestaNombre, List<Candidato> candidatos) {}

    public record Candidato(UUID id, String codigo, String nombre) {}
}
