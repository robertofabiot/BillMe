package com.example.billme.importacion;

import java.util.List;

/**
 * Resultado de leer el archivo: la hoja donde estaba la lista, sus filas útiles
 * y cuántas se ignoraron (sin código o descripción, o con código repetido).
 */
public record LecturaListaPrecios(
        String hoja,
        List<FilaListaPrecios> filas,
        int filasIgnoradas
) {}
