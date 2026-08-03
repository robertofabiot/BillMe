package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupador de facturas para un cliente.
 * Permite generar estados de cuenta personalizados con subtotales
 * sin modificar ni duplicar los datos reales de facturación.
 */
@Entity
@Table(name = "consolidados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consolidado extends BaseEntity {

    /**
     * Folio identificador visible (ej. "CONS-0001").
     */
    @Column(name = "folio_interno", unique = true, nullable = false, length = 20)
    private String folioInterno;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "consolidado", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ConsolidadoGroup> grupos = new ArrayList<>();
}
