package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago parcial o total registrado contra una factura.
 * Una factura puede acumular múltiples abonos hasta cubrir su total.
 */
@Entity
@Table(name = "abonos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abono extends BaseEntity {

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Fecha en que el cliente realizó el pago (puede diferir de la fecha de registro).
     */
    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
}
