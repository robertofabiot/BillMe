package com.example.billme.model;

import com.example.billme.enums.FuentePrecio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un precio de lista del proveedor (sin IVA) vigente desde una fecha.
 * El precio vigente de un producto es su registro más reciente.
 */
@Entity
@Table(name = "precios_lista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrecioLista extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "precio_sin_iva", nullable = false, precision = 12, scale = 4)
    private BigDecimal precioSinIva;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente", nullable = false, length = 30)
    private FuentePrecio fuente;

    /** Descuento usado para calcular la lista (del proveedor o del cliente), en porcentaje. */
    @Column(name = "descuento_pct", precision = 5, scale = 2)
    private BigDecimal descuentoPct;

    /** Precio del que se partió para calcular la lista (factura del proveedor o precio a un cliente). */
    @Column(name = "precio_origen", precision = 12, scale = 4)
    private BigDecimal precioOrigen;

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;
}
