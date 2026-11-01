package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de una importación de la lista de precios (vista previa o aplicada).
 * {@code muestra} trae hasta 50 productos nuevos o con precio distinto para revisar.
 */
public record ImportacionListaResponse(
        String hoja,
        int filasLeidas,
        int filasIgnoradas,
        int productosNuevos,
        int preciosQueCambian,
        int sinCambios,
        int sinPrecio,
        List<Cambio> muestra,
        boolean aplicada
) {
    public record Cambio(String codigo, String descripcion, BigDecimal precioAnterior, BigDecimal precioNuevo) {}
}
