package com.example.billme.importacion;

import java.math.BigDecimal;

/** Una fila útil de la lista de precios del proveedor. Peso y precio pueden faltar. */
public record FilaListaPrecios(
        int numeroFila,
        String codigo,
        String descripcion,
        String unidad,
        BigDecimal peso,
        BigDecimal precio
) {}
