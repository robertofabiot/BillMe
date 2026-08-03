package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de detalle de una factura.
 * Registra el precio acordado con el cliente en el momento de la venta.
 * También guarda el costo unitario del producto en ese momento (snapshot)
 * para cálculo histórico de márgenes de ganancia.
 */
@Entity
@Table(name = "detalles_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleFactura extends BaseEntity {

    /**
     * Permite valores decimales (metros, yardas, etc.)
     */
    @Column(name = "cantidad", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    /**
     * Precio pactado con el cliente. Puede diferir del precio de lista del producto.
     */
    @Column(name = "precio_unitario_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioVenta;

    /**
     * Snapshot del costo neto del producto al momento de la venta.
     * Permite calcular el margen de ganancia real sin afectar el flujo de ventas.
     */
    @Column(name = "costo_unitario_snapshot", precision = 12, scale = 2)
    private BigDecimal costoUnitarioSnapshot;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
}
