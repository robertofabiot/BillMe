package com.example.billme.model;

import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cotización o factura. Comparten estructura; cada tipo tiene su serie de folios (COT-/FAC-).
 * Totales y pesos los calcula siempre el backend (ver CalculoDocumento).
 */
@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDocumento tipo;

    /** Folio visible: COT-0001, FAC-0001. */
    @Column(name = "folio", unique = true, nullable = false, length = 20)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDocumento estado;

    /** Fecha del documento (la que se imprime y filtra), no la de registro. */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "total_kg", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalKg = BigDecimal.ZERO;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    /** Si al exportar se muestran las columnas de fecha y lugar de entrega. */
    @Column(name = "mostrar_entrega", nullable = false)
    @Builder.Default
    private boolean mostrarEntrega = false;

    /** Si al exportar y visualizar se muestra el total en dólares. */
    @Column(name = "mostrar_total_usd", nullable = false)
    @Builder.Default
    private boolean mostrarTotalUsd = false;

    /** Tipo de cambio del momento en que se creó el documento. */
    @Column(name = "tipo_cambio", nullable = false, precision = 12, scale = 6)
    private BigDecimal tipoCambio;

    /** Identifica lo importado desde Excel, para no duplicarlo al reimportar. */
    @Column(name = "import_key", unique = true, length = 64)
    private String importKey;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    /** Usuario que lo creó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** En una factura: la cotización de la que salió. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_origen_id")
    private Documento cotizacionOrigen;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<DocumentoLinea> lineas = new ArrayList<>();
}
