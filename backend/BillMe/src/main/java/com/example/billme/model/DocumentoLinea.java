package com.example.billme.model;

import com.example.billme.enums.MetodoPrecio;
import com.example.billme.util.NormalizadorTexto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Línea de una cotización o factura. Guarda su propia descripción (la que se imprime):
 * en los productos genéricos (placas a medida, línea libre) es lo que identifica la venta.
 */
@Entity
@Table(name = "documento_lineas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoLinea extends BaseEntity {

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    /** Descripción normalizada para buscar precios de productos genéricos. Se calcula sola. */
    @Column(name = "descripcion_norm", nullable = false, length = 500)
    private String descripcionNorm;

    /** Admite decimales (metros, kilos). */
    @Column(name = "cantidad", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "peso_unit_kg", precision = 10, scale = 2)
    private BigDecimal pesoUnitKg;

    @Column(name = "peso_total_kg", precision = 12, scale = 2)
    private BigDecimal pesoTotalKg;

    /** Precio unitario con IVA, redondeado a 2 decimales. */
    @Column(name = "precio_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnit;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_precio", nullable = false, length = 20)
    @Builder.Default
    private MetodoPrecio metodoPrecio = MetodoPrecio.MANUAL;

    /** Datos con los que se calculó el precio (JSON), para repetir el cálculo al editar. */
    @Column(name = "parametros", columnDefinition = "TEXT")
    private String parametros;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "lugar_entrega", length = 200)
    private String lugarEntrega;

    /** Costo unitario estimado con IVA al momento de la venta (para calcular la ganancia). */
    @Column(name = "costo_unit_con_iva", precision = 12, scale = 2)
    private BigDecimal costoUnitConIva;

    @Column(name = "import_key", unique = true, length = 64)
    private String importKey;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @PrePersist
    @PreUpdate
    void normalizar() {
        descripcionNorm = NormalizadorTexto.normalizar(descripcion);
    }
}
