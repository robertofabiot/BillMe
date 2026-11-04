package com.example.billme.dto;

import java.time.Instant;

/** Una versión anterior de un documento: cómo estaba, cuándo se editó y quién lo hizo. */
public record RevisionResponse(
        int numero,
        Instant editadoEn,
        String editadoPor,
        DocumentoResponse documento
) {}
