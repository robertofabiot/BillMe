package com.example.billme.model;

import com.example.billme.enums.FormaCalculo;
import com.example.billme.util.NormalizadorTexto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Producto del catálogo. No guarda precio: el precio de lista vigente está en
 * {@link PrecioLista} y el precio a cada cliente sale de su historial de ventas.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends BaseEntity {

    /** Los códigos del proveedor principal tienen 9 dígitos y los 3 primeros indican la familia. */
    private static final Pattern CODIGO_PROVEEDOR = Pattern.compile("\\d{9}");

    @Column(name = "codigo_interno", unique = true, nullable = false, length = 50)
    private String codigoInterno;

    @Column(name = "nombre_principal", nullable = false, length = 200)
    private String nombrePrincipal;

    /** Nombre normalizado para buscar (ver NormalizadorTexto). Se calcula solo. */
    @Column(name = "nombre_norm", nullable = false, length = 200)
    private String nombreNorm;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "unidad", nullable = false, length = 20)
    @Builder.Default
    private String unidad = "UND";

    /** Familia del proveedor (prefijo del código); agrupa productos con el mismo descuento. */
    @Column(name = "familia", length = 20)
    private String familia;

    /** Peso unitario en kg. */
    @Column(name = "peso", precision = 10, scale = 3)
    private BigDecimal peso;

    /** Genérico: placas a medida, línea libre. Se buscan por la descripción de cada venta. */
    @Column(name = "generico", nullable = false)
    @Builder.Default
    private boolean generico = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_calculo", nullable = false, length = 20)
    @Builder.Default
    private FormaCalculo formaCalculo = FormaCalculo.NINGUNA;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AliasProducto> aliases = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void calcularCamposDerivados() {
        nombreNorm = NormalizadorTexto.normalizar(nombrePrincipal);
        if (familia == null && codigoInterno != null && CODIGO_PROVEEDOR.matcher(codigoInterno).matches()) {
            familia = codigoInterno.substring(0, 3);
        }
    }
}
