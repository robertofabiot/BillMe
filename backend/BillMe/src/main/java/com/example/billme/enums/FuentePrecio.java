package com.example.billme.enums;

/** De dónde salió un precio de lista. */
public enum FuentePrecio {
    /** Importado de la lista de precios del proveedor. */
    LISTA_IMPORTADA,
    /** Calculado desde el precio que facturó el proveedor y su descuento. */
    FACTURA_PROVEEDOR,
    /** Calculado desde el precio dado a un cliente y su descuento. */
    PRECIO_CLIENTE,
    /** Escrito a mano. */
    MANUAL
}
