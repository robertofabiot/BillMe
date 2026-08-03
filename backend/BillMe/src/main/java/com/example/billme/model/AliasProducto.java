package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Alias o nombre alternativo para un producto.
 * Permite buscarlo por múltiples nombres o especificaciones (ej. "Tubo 1/2", "Tubo PVC media pulgada").
 */
@Entity
@Table(name = "alias_productos", indexes = {
        @Index(name = "idx_alias_nombre", columnList = "nombre_alias")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AliasProducto extends BaseEntity {

    @Column(name = "nombre_alias", nullable = false, length = 200)
    private String nombreAlias;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
}
