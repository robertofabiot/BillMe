package com.example.billme.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un producto del catálogo.
 * Contiene la lógica de precios de proveedor y costo neto.
 *
 * Fórmula de costo neto:
 *   costoNeto = precioListaProveedor * (1 - descuentoProveedor/100) * (1 + iva/100)
 * El iva es el configurado globalmente (por defecto 15%).
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends BaseEntity {

    @Column(name = "codigo_interno", unique = true, nullable = false, length = 50)
    private String codigoInterno;

    @Column(name = "nombre_principal", nullable = false, length = 200)
    private String nombrePrincipal;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Precio de lista publicado por el proveedor, sin IVA.
     */
    @Column(name = "precio_lista_proveedor", precision = 12, scale = 2)
    private BigDecimal precioListaProveedor;

    /**
     * Porcentaje de descuento otorgado por el proveedor (0-100).
     */
    @Column(name = "descuento_proveedor", precision = 5, scale = 2)
    private BigDecimal descuentoProveedor;

    /**
     * Costo neto real para el negocio: precio lista * (1 - descuento%) * (1 + iva%).
     * Se almacena calculado para búsquedas y reportes rápidos.
     */
    @Column(name = "costo_neto", precision = 12, scale = 2)
    private BigDecimal costoNeto;

    @Column(name = "peso", precision = 10, scale = 3)
    private BigDecimal peso;

    // ── Relaciones ──────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AliasProducto> aliases = new ArrayList<>();
}
