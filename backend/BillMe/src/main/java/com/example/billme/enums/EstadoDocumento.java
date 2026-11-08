package com.example.billme.enums;

/**
 * Estado de un documento.
 * Cotización: PENDIENTE → ACEPTADA (al convertirse en factura) o RECHAZADA.
 * Factura: ACTIVA o ANULADA. Solo las facturas activas cuentan como ingresos.
 * Histórico: HISTORICA. Registro del histórico de precios (solo lectura hasta convertirse en factura).
 */
public enum EstadoDocumento {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    ACTIVA,
    ANULADA,
    HISTORICA
}
