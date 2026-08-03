package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloque/subtotal dentro de un Consolidado.
 * El nombre es editable libremente (ej. "Materiales Julio", "FAC-0001").
 */
@Entity
@Table(name = "consolidado_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidadoGroup extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    /**
     * Orden de aparición del grupo dentro del consolidado (0-based).
     */
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidado_id", nullable = false)
    private Consolidado consolidado;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ConsolidadoItem> items = new ArrayList<>();
}
