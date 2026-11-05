package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago registrado a la cuenta del cliente (con proyecto opcional).
 */
@Entity
@Table(name = "abonos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abono extends BaseEntity {

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    /**
     * Fecha en que el cliente realizó el pago (puede diferir de la fecha de registro).
     */
    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;
}
