package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidado armado en vivo desde las facturas de un cliente/proyecto
 * dentro de un rango de fechas. Solo se guardan el rango y los nombres editados de grupos.
 */
@Entity
@Table(name = "consolidados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consolidado extends BaseEntity {

    @Column(name = "folio", unique = true, nullable = false, length = 20)
    private String folio;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @Column(name = "desde", nullable = false)
    private LocalDate desde;

    @Column(name = "hasta", nullable = false)
    private LocalDate hasta;

    /** Si al exportar y visualizar se muestra el total en dólares. */
    @Column(name = "mostrar_total_usd", nullable = false)
    @Builder.Default
    private boolean mostrarTotalUsd = false;

    /** Tipo de cambio del momento en que se creó el consolidado. */
    @Column(name = "tipo_cambio", nullable = false, precision = 12, scale = 6)
    private java.math.BigDecimal tipoCambio;

    @OneToMany(mappedBy = "consolidado", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaEntrega ASC")
    @Builder.Default
    private List<ConsolidadoGrupo> grupos = new ArrayList<>();
}
