package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa a un cliente del negocio.
 * Un cliente puede tener múltiples proyectos, facturas y consolidados.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Notas libres sobre el cliente (metodología de pago, observaciones, etc.)
     */
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal descuentoPct = java.math.BigDecimal.ZERO;

    @Column(name = "tarifa_kg_usd", precision = 8, scale = 4)
    private java.math.BigDecimal tarifaKgUsd;

    // ── Relaciones ──────────────────────────────────────────────────────────

    /**
     * Los proyectos se borran con el cliente. Las facturas y consolidados no se
     * mapean aquí a propósito: nunca deben borrarse en cascada (ver ClienteService).
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Proyecto> proyectos = new ArrayList<>();
}
