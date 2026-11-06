package com.example.billme.dto;

import java.time.Instant;

/** Una versión anterior de un documento: cómo estaba, cuándo se editó, quién lo hizo y por qué. */
public record RevisionResponse(
        int numero,
        Instant editadoEn,
        String editadoPor,
        String motivo,
        DocumentoResponse documento
) {}
