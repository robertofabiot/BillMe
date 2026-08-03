package com.example.billme.model;

import com.example.billme.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una transacción de venta o cotización.
 * Puede ser una Proforma (COTIZACION) o una venta real en cualquiera de sus estados.
 * La asociación a un Proyecto es opcional.
 */
@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura extends BaseEntity {

    /**
     * Folio de identificación visible para el usuario (ej. "FAC-0001").
     */
    @Column(name = "folio_interno", unique = true, nullable = false, length = 20)
    private String folioInterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoFactura estado = EstadoFactura.COTIZACION;

    /**
     * Total calculado a partir de los detalles. Se recalcula al guardar/modificar.
     */
    @Column(name = "total_venta", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalVenta = BigDecimal.ZERO;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /**
     * Relación opcional: si es NULL, la factura pertenece al cliente de forma general.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    /**
     * Usuario que creó o registró la factura.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleFactura> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Abono> abonos = new ArrayList<>();
}
