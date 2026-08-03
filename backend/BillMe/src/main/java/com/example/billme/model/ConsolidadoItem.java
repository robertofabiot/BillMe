package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ítem individual dentro de un grupo de un Consolidado.
 * Puede referenciar una factura original pero sus datos (nombre, precio)
 * son independientes — editables sin afectar la factura original.
 */
@Entity
@Table(name = "consolidado_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoItem extends BaseEntity {

    /**
     * Nombre del producto o servicio. Editable independientemente del catálogo.
     */
    @Column(name = "producto_nombre", nullable = false, length = 200)
    private String productoNombre;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Orden de aparición dentro del grupo (0-based). Permite reordenamiento visual.
     */
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    // ── Relaciones ──────────────────────────────────────────────────────────

    /**
     * Referencia opcional a la factura de la que proviene este ítem.
     * NULL indica que fue añadido manualmente al consolidado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private ConsolidadoGroup grupo;
}
