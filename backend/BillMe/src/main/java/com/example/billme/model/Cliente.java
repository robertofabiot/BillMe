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

    // ── Relaciones ──────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Proyecto> proyectos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Factura> facturas = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consolidado> consolidados = new ArrayList<>();
}
