package com.example.billme.model;

import com.example.billme.enums.EstadoProyecto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa facturas de un mismo cliente bajo una obra o trabajo específico.
 * Modelo minimalista: solo nombre y estado (sin presupuestos ni fechas estimadas).
 */
@Entity
@Table(name = "proyectos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proyecto extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoProyecto estado = EstadoProyecto.ACTIVO;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "proyecto")
    @Builder.Default
    private List<Factura> facturas = new ArrayList<>();
}
